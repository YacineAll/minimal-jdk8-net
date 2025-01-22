from pyspark.sql import SparkSession
from pyspark.sql.functions import lit

# Initialize SparkSession
spark = SparkSession.builder \
    .appName("Count Rows by Path") \
    .getOrCreate()

# List of paths to process
paths = [
    "/path/to/data1",
    "/path/to/data2",
    "/path/to/data3"
]

# Initialize an empty DataFrame to store results
results_df = spark.createDataFrame([], schema="path STRING, count LONG")

# Iterate over each path and count rows
for path in paths:
    try:
        # Read data from the path
        df = spark.read.format("parquet").load(path)  # Change format if needed (e.g., csv, json, etc.)
        
        # Count the rows in the current DataFrame
        row_count = df.count()
        
        # Create a DataFrame with the path and count
        path_df = spark.createDataFrame([(path, row_count)], schema=["path", "count"])
        
        # Append to the results DataFrame
        results_df = results_df.union(path_df)
    except Exception as e:
        print(f"Error processing path {path}: {e}")

# Write the results to a CSV file
results_df.write.csv("/path/to/output/counts.csv", header=True)

# Stop the SparkSession
spark.stop()