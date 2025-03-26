import re

# Lis le contenu du fichier with open("process_configurations.py", "r") as file:

content = file.read()

# Regex pour extraire les chaînes entre

guillemets

matches = re.findall(r'"[^"]+",

content)

# Nettoie les guillemets et backslashes

output = [m.strip('"').replace('\\\\', '\\') for m in matches]

# Affiche le tableau résultant

print(output)