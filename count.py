import subprocess
import json

def load_bash_arrays(file_path):
    # Use subprocess to source the file and extract variables
    cmd = f"source {file_path} && echo $(declare -p password certificats)"
    result = subprocess.run(cmd, shell=True, text=True, capture_output=True)
    
    if result.returncode != 0:
        raise RuntimeError(f"Failed to load Bash file: {result.stderr}")
    
    bash_output = result.stdout.strip()
    
    # Parsing Bash output to extract array values
    env = {}
    exec(bash_output.replace("declare -a ", "").replace("=", " = "), {}, env)

    # Convert to Python dictionary
    def parse_array(arr):
        parsed_dict = {}
        for item in arr:
            key, value = item.split("::", 1)
            parsed_dict[key] = value
        return parsed_dict
    
    password_dict = parse_array(env['password'])
    certificats_dict = parse_array(env['certificats'])
    
    return {"password": password_dict, "certificats": certificats_dict}

# Example usage
bash_file = "example.sh"
data = load_bash_arrays(bash_file)
print(json.dumps(data, indent=2))