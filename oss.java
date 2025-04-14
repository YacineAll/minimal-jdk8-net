def merge_lists(*lists):
    # Convert all lists to sets
    sets = [set(lst) for lst in lists]
    merged = []
    unmergeable = []
    
    while sets:
        current = sets.pop(0)
        merged_any = False
        
        for i, other in enumerate(merged):
            # Check for common elements
            if current & other:
                # Merge them (union)
                merged[i] = current | other
                merged_any = True
                break
                
        if not merged_any:
            for i, remaining in enumerate(sets):
                if current & remaining:
                    # Merge and remove the other set from remaining lists
                    merged.append(current | remaining)
                    del sets[i]
                    merged_any = True
                    break
                    
        if not merged_any:
            if not merged:
                merged.append(current)
            else:
                unmergeable.append(current)
    
    return merged, unmergeable

# Example usage:
a = [1, 2]
b = [3]
c = [2, 3]

merged, unmergeable = merge_lists(a, b, c)

print("Merged sets:", merged)        # Output: [{1, 2, 3}]
print("Unmergeable sets:", unmergeable)  # Output: []
