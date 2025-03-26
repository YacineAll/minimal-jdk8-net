def parse_bash_array(bash_content):
    """
    Parse a bash-style array declaration into a Python dictionary.
    
    Args:
        bash_content (str): Content of the bash script containing array declarations
    
    Returns:
        dict: A dictionary with parsed arrays
    """
    import re
    
    # Dictionary to store parsed arrays
    parsed_arrays = {}
    
    # Regular expression to match array declarations
    array_pattern = re.compile(r'(\w+)=\(\s*(.*?)\s*\)', re.DOTALL)
    # Regular expression to match individual array elements
    element_pattern = re.compile(r'"([^"]+)"')
    
    # Find all array declarations
    for array_match in array_pattern.finditer(bash_content):
        array_name = array_match.group(1)
        array_content = array_match.group(2)
        
        # Parse individual elements
        elements = element_pattern.findall(array_content)
        
        # Convert to dictionary
        parsed_arrays[array_name] = {
            item.split('::')[0]: item.split('::')[1] 
            for item in elements
        }
    
    return parsed_arrays

# Read the bash script
with open('allsecrets.sh', 'r') as file:
    bash_content = file.read()

# Parse the bash arrays
result = parse_bash_array(bash_content)

# Print the result
print(result)
