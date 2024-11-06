import org.apache.spark.sql.types.*;
import java.util.regex.*;
import java.util.*;

public class SchemaChecker {

    public static boolean doesPathExist(StructType schema, String path) {
        String[] parts = path.split("\\.");
        return checkPath(schema, Arrays.asList(parts), 0);
    }

    private static boolean checkPath(DataType schema, List<String> parts, int idx) {
        if (idx >= parts.size()) return true; // End of path, exists

        String part = parts.get(idx);
        Matcher matcher = Pattern.compile("([a-zA-Z_0-9]+)(\\[(\\d+)\\])?").matcher(part);

        if (!matcher.matches()) return false;

        String fieldName = matcher.group(1);
        String arrayIdx = matcher.group(3);

        if (!(schema instanceof StructType)) return false;

        StructType struct = (StructType) schema;
        StructField field = struct.getFieldIndex(fieldName).map(struct::apply).orElse(null);

        if (field == null) return false;

        if (arrayIdx != null) {
            if (!(field.dataType() instanceof ArrayType)) return false;
            return checkPath(((ArrayType) field.dataType()).elementType(), parts, idx + 1);
        } else {
            return checkPath(field.dataType(), parts, idx + 1);
        }
    }

    public static void main(String[] args) {
        StructType schema = new StructType()
            .add("payload", new StructType()
                .add("names", new ArrayType(new StructType()
                    .add("lastName", new StructType()
                        .add("fr", DataTypes.StringType)
                    ), true)
                )
                .add("lastNames", new ArrayType(DataTypes.StringType, true))
            );

        System.out.println(doesPathExist(schema, "payload.names[0].lastName.fr")); // true
        System.out.println(doesPathExist(schema, "payload.lastNames[1]")); // true
        System.out.println(doesPathExist(schema, "payload.unknownField")); // false
    }
}
