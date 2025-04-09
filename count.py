import re

def parse_complex_string_to_dict(s):
    # Remove leading/trailing whitespace
    s = s.strip()
    
    # Handle empty case
    if s == "{}" or not s:
        return {}
    
    # Remove outer braces
    if s.startswith("{") and s.endswith("}"):
        s = s[1:-1]
    
    # Empty dictionary after removing braces
    if not s:
        return {}
    
    result = {}
    
    # Track position and nesting level
    pos = 0
    length = len(s)
    
    while pos < length:
        # Find key
        key_end = s.find("=", pos)
        if key_end == -1:
            break
            
        key = s[pos:key_end].strip()
        pos = key_end + 1
        
        # Handle value (could be simple or nested)
        if pos < length and s[pos] == "{":
            # Nested value
            nesting_level = 1
            start_pos = pos
            pos += 1
            
            while pos < length and nesting_level > 0:
                if s[pos] == "{":
                    nesting_level += 1
                elif s[pos] == "}":
                    nesting_level -= 1
                pos += 1
            
            value_str = s[start_pos:pos]
            value = parse_complex_string_to_dict(value_str)
        else:
            # Simple value or empty
            next_brace = s.find("{", pos)
            next_comma = s.find(",", pos)
            
            if next_comma != -1 and (next_brace == -1 or next_comma < next_brace):
                value = s[pos:next_comma].strip()
                pos = next_comma + 1
            else:
                value = s[pos:].strip()
                pos = length
        
        result[key] = value
        
        # Skip any comma or whitespace
        while pos < length and (s[pos] == "," or s[pos].isspace()):
            pos += 1
    
    return result

# Test with your example
test_string = "{payload={payment={val=EEU}}}"
result = parse_complex_string_to_dict(test_string)
print(result)

# More complex test with deeper nesting and multiple key-values
complex_test = "{user={name=John, profile={interests={sports={football=true, basketball=false}, reading=true}, location={city=New York, country=USA}}}, settings={darkMode=true, notifications={email=daily, push=true}}}"
complex_result = parse_complex_string_to_dict(complex_test)
print(complex_result)
