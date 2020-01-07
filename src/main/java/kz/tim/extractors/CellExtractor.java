package kz.tim.extractors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.tim.models.row.cell.CellData;
import kz.tim.models.row.cell.CellType;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.FieldIdentifier;
import org.apache.cassandra.cql3.Json;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.CounterColumnType;
import org.apache.cassandra.db.marshal.ListType;
import org.apache.cassandra.db.marshal.MapType;
import org.apache.cassandra.db.marshal.SetType;
import org.apache.cassandra.db.marshal.TupleType;
import org.apache.cassandra.db.marshal.UserType;
import org.apache.cassandra.db.partitions.Partition;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.CellPath;
import org.apache.cassandra.db.rows.ComplexColumnData;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.transport.ProtocolVersion;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Class responsible for extracting cells information from row update.
 *
 * @author Timur Tibeyev.
 */
public class CellExtractor {
    private Partition update;
    private Map<String, Object> dataMap = new HashMap<>();
    private Map<String, Object> deletedDataMap = new HashMap<>();
    private Set<String> deletedColumns = new HashSet<>();
    private Map<String, CellType> columnTypeMap = new HashMap<>();
    private ObjectMapper mapper = new ObjectMapper();
    private TypeReference typeRef = new TypeReference<HashMap<String, Object>>() {};

    public CellExtractor(Partition update) {
        this.update = update;
    }

    /**
     * Extracts cell modifications from {@link Row} object.
     *
     * @return list of modified cells.
     */
    public List<CellData> extractCells(Row row) {
        for (Cell cell: row.cells()) {
            if (cell.column().type instanceof UserType) {
                processUserTypeCell(row, cell);
            } else
            if (cell.column().type instanceof ListType) {
                processListTypeCell(cell);
            } else
            if (cell.column().type instanceof SetType) {
                processSetTypeCell(cell);
            } else
            if (cell.column().type instanceof MapType) {
                processMapTypeCell(cell);
            } else
            if (cell.column().type instanceof TupleType) {
                processTupleTypeCell(cell);
            } else
            if (cell.column().type instanceof CounterColumnType) {
                processCounterTypeCell(cell);
            } else {
                processSimpleCell(cell);
            }
        }
        return constructCellList(row);
    }

    private List<CellData> constructCellList(Row row) {
        List<CellData> result = new LinkedList<>();
        Set<String> columns = new HashSet<>();
        for (Cell cell: row.cells()) {
            String columnName = cell.column().name.toString();
            if (!columns.contains(columnName)) {
                columns.add(columnName);
                CellData cellData = new CellData();
                cellData.setName(columnName);
                cellData.setType(columnTypeMap.getOrDefault(columnName, CellType.SIMPLE));
                if (deletedColumns.contains(columnName)) {
                    cellData.setDeleted(true);

                } else {
                    cellData.setValue(dataMap.get(columnName));
                    cellData.setDeletedItems(deletedDataMap.get(columnName));
                }
                result.add(cellData);
            }
        }

        result.addAll(getDeletedComplexCells(row));
        return result;
    }

    private  List<CellData> getDeletedComplexCells(Row row) {
        return row.columns().stream().filter(it -> it.isComplex()
                && !row.getComplexColumnData(it).complexDeletion().isLive()
                && !columnTypeMap.containsKey(it.name.toString())).map(cf -> {
                    CellData cellData = new CellData();
                    cellData.setName(cf.name.toString());
                    cellData.setDeleted(true);
                    cellData.setType(getCellType(cf));
                    return cellData;
                }).collect(Collectors.toList());
    }

    private CellType getCellType(ColumnDefinition cf) {
        CellType cellType;
        if (cf.type instanceof UserType) {
            cellType = CellType.UDT;
        } else
        if (cf.type instanceof UserType) {
            cellType = CellType.LIST;
        } else
        if (cf.type instanceof UserType) {
            cellType = CellType.SET;
        } else
        if (cf.type instanceof UserType) {
            cellType = CellType.MAP;
        } else
        if (cf.type instanceof UserType) {
            cellType = CellType.TUPLE;
        } else {
            cellType = CellType.SIMPLE;
        }
        return cellType;
    }

    private void  processSimpleCell(Cell cell) {
        String columnName = cell.column().name.toString();
        AbstractType cellValueType = cell.column().cellValueType();
        Object cellValue = cellValueType.compose(cell.value());
        columnTypeMap.put(columnName, CellType.SIMPLE);

        if (cell.isLive(0)) {
            dataMap.put(columnName, cellValue);
        } else {
            deletedColumns.add(columnName);
        }
    }

    private void processCounterTypeCell(Cell cell) {
        String columnName = cell.column().name.toString();
        columnTypeMap.put(columnName, CellType.COUNTER);
        CounterColumnType cellValueType = (CounterColumnType) cell.column().cellValueType();

        if (cell.isLive(0)) {
            Object cellValue = cellValueType.compose(cell.value());
            dataMap.put(columnName, cellValue);
        } else {
            deletedColumns.add(columnName);
        }
    }

    private void processTupleTypeCell(Cell cell) {
        String columnName = cell.column().name.toString();
        columnTypeMap.put(columnName, CellType.TUPLE);
        if (cell.isLive(0)) {
            TupleType tupleType = (TupleType) cell.column().type;
            ByteBuffer[] splitByteBuffer = tupleType.split(cell.value());
            for (int i = 0; i < tupleType.allTypes().size(); i++) {
                AbstractType valueType = tupleType.allTypes().get(i);
                Object cellValue = valueType.compose(splitByteBuffer[i]);

                List<Object> list = (List<Object>) dataMap
                        .getOrDefault(columnName, new LinkedList<>());
                list.add(getMapOrValue(valueType, splitByteBuffer[i], cellValue));
            }
        } else {
            deletedColumns.add(columnName);
        }
    }

    private void processMapTypeCell(Cell cell) {
        String columnName = cell.column().name.toString();
        MapType columnType = (MapType) cell.column().type;
        AbstractType cellValueType = cell.column().cellValueType();
        Object cellValue = cellValueType.compose(cell.value());
        columnTypeMap.put(columnName, CellType.MAP);

        AbstractType keysType = columnType.getKeysType();
        CellPath path = cell.path();
        for (int i = 0; i < path.size(); i++) {
            ByteBuffer byteBuffer = path.get(i);
            Object cellKey = keysType.compose(byteBuffer);

            if (cell.isLive(0)) {
                Map<Object, Object> map = (Map<Object, Object>) dataMap
                        .getOrDefault(columnName, new HashMap<>());
                map.put(cellKey, getMapOrValue(cellValueType, cell.value(), cellValue));
            } else {
                Map<Object, Object> map = (Map<Object, Object>) deletedDataMap
                        .getOrDefault(columnName, new HashMap<>());
                map.put(cellKey, getMapOrValue(cellValueType, cell.value(), cellValue));
            }
        }
    }

    private void processSetTypeCell(Cell cell) {
        String columnName = cell.column().name.toString();
        SetType columnType = (SetType) cell.column().type;
        columnTypeMap.put(columnName, CellType.SET);

        CellPath path = cell.path();
        for (int i = 0; i < path.size(); i++) {
            ByteBuffer byteBuffer = path.get(i);
            AbstractType keysType = columnType.getElementsType();
            Object cellValue = keysType.compose(byteBuffer);

            if (cell.isLive(0)) {
                Set<Object> set = (Set<Object>) dataMap
                        .getOrDefault(columnName, new HashSet<>());
                set.add(getMapOrValue(keysType, cell.value(), cellValue));
            } else {
                Set<Object> set = (Set<Object>) deletedDataMap
                        .getOrDefault(columnName, new HashSet<>());
                set.add(getMapOrValue(keysType, cell.value(), cellValue));
            }
        }
    }

    private void processListTypeCell(Cell cell) {
        String columnName = cell.column().name.toString();
        AbstractType cellValueType = cell.column().cellValueType();
        Object cellValue = cellValueType.compose(cell.value());
        columnTypeMap.put(columnName, CellType.LIST);

        if (cell.isLive(0)) {
            List<Object> list = (List<Object>) dataMap.getOrDefault(columnName, new LinkedList<>());
            list.add(getMapOrValue(cellValueType, cell.value(), cellValue));
        } else {
            deletedColumns.add(columnName);
        }
    }

    private void processUserTypeCell(Row row, Cell cell) {
        ColumnDefinition columnDef = cell.column();
        String columnName = columnDef.name.toString();
        UserType columnType = (UserType) columnDef.type;
        columnTypeMap.put(columnName, CellType.UDT);

        if (cell.isLive(0)) {
            if (columnDef.isComplex()) {
                ComplexColumnData complexColumnData = row.getComplexColumnData(columnDef);
                dataMap.put(columnName, extractUserType(columnType, complexColumnData));
            } else {
                dataMap.put(columnName, getValueAsMap(columnType, cell.value()));
            }
        } else {
            deletedColumns.add(columnName);
        }
    }

    private Map<String, Object> extractUserType(UserType userType,
                                                ComplexColumnData complexColumnData) {
        Map<String, Object> res = new HashMap<>();
        for (int i = 0; i < userType.size(); i++) {
            AbstractType cellType = userType.fieldType(i);
            FieldIdentifier cellName = userType.fieldName(i);
            CellPath cellPath = userType.cellPathForField(cellName);
            Cell cell = complexColumnData.getCell(cellPath);

            res.put(Json.quoteAsJsonString(cellName.toString()),
                    getValueAsMap(cellType, cell.value()));
        }
        return res;
    }

    private Map<Object, Object> getValueAsMap(AbstractType keyType, ByteBuffer byteBuffer) {
        String json = keyType.toJSONString(byteBuffer,
                ProtocolVersion.V4);
        try {
            return mapper.readValue(json, typeRef);
        } catch (JsonParseException e) {
            throw new RuntimeException(e);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Object getMapOrValue(AbstractType keyType, ByteBuffer byteBuffer, Object defaultValue) {
        if (keyType instanceof UserType) {
            return getValueAsMap(keyType, byteBuffer);
        } else {
            return defaultValue;
        }
    }
}
