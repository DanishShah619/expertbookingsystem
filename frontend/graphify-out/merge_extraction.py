import sys, json
from pathlib import Path

# Read AST and semantic results
ast_data = json.loads(Path('graphify-out/.graphify_ast.json').read_text())
sem_data = json.loads(Path('graphify-out/.graphify_semantic.json').read_text())

# Deduplicate nodes by id
seen = {n['id'] for n in ast_data['nodes']}
all_nodes = list(ast_data['nodes'])
for n in sem_data['nodes']:
    if n['id'] not in seen:
        all_nodes.append(n)
        seen.add(n['id'])

# Merge edges and hyperedges
all_edges = ast_data['edges'] + sem_data.get('edges', [])
all_hyperedges = sem_data.get('hyperedges', [])

# Create merged extraction
merged = {
    'nodes': all_nodes,
    'edges': all_edges,
    'hyperedges': all_hyperedges,
    'input_tokens': sem_data.get('input_tokens', 0),
    'output_tokens': sem_data.get('output_tokens', 0),
}

Path('graphify-out/.graphify_extract.json').write_text(json.dumps(merged, indent=2))
total = len(all_nodes)
edges = len(all_edges)
print(f'Merged: {total} nodes, {edges} edges ({len(ast_data["nodes"])} AST + {len(sem_data["nodes"])} semantic)')
