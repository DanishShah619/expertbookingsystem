if __name__ == '__main__':
    import sys, json
    from graphify.build import build_from_json
    from graphify.cluster import score_all
    from graphify.analyze import suggest_questions
    from graphify.report import generate
    from pathlib import Path

    extraction = json.loads(Path('graphify-out/.graphify_extract.json').read_text())
    detection_text = Path('graphify-out/.graphify_detect.json').read_text(encoding='utf-16')
    detection = json.loads(detection_text)
    analysis = json.loads(Path('graphify-out/.graphify_analysis.json').read_text())

    G = build_from_json(extraction)
    communities = {int(k): v for k, v in analysis['communities'].items()}
    cohesion = {int(k): v for k, v in analysis['cohesion'].items()}
    tokens = {'input': extraction.get('input_tokens', 0), 'output': extraction.get('output_tokens', 0)}

    # Auto-generate labels from node names
    labels = {}
    node_data = {n['id']: n for n in extraction['nodes']}
    
    for cid, members in communities.items():
        # Get node labels in this community
        node_labels = [node_data.get(nid, {}).get('label', nid) for nid in members if nid in node_data]
        node_labels = node_labels[:5]  # Sample first 5
        
        # Auto-label based on patterns
        if any('api' in l.lower() and 'admin' in l.lower() for l in node_labels):
            labels[cid] = "Admin API"
        elif any('api' in l.lower() and 'expert' in l.lower() for l in node_labels):
            labels[cid] = "Expert APIs"
        elif any('page' in l.lower() and 'admin' in l.lower() for l in node_labels):
            labels[cid] = "Admin Pages"
        elif any('page' in l.lower() and 'expert' in l.lower() for l in node_labels):
            labels[cid] = "Expert Pages"
        elif any('component' in l.lower() for l in node_labels):
            labels[cid] = "UI Components"
        elif any('type' in l.lower() or 'dto' in l.lower() for l in node_labels):
            labels[cid] = "API Types"
        elif any('api' in l.lower() and ('slot' in l.lower() or 'booking' in l.lower()) for l in node_labels):
            labels[cid] = "Booking & Slots"
        elif any('mock' in l.lower() or 'data' in l.lower() for l in node_labels):
            labels[cid] = "Mock Data"
        elif any('route' in l.lower() or 'config' in l.lower() for l in node_labels):
            labels[cid] = "Routes & Config"
        elif any('websocket' in l.lower() or 'stream' in l.lower() for l in node_labels):
            labels[cid] = "WebSocket & Streams"
        elif any('icon' in l.lower() or 'logo' in l.lower() or 'svg' in l.lower() for l in node_labels):
            labels[cid] = "UI Assets"
        elif any('next.js' in l.lower() or 'vercel' in l.lower() for l in node_labels):
            labels[cid] = "Framework & Deployment"
        elif any('auth' in l.lower() or 'user' in l.lower() for l in node_labels):
            labels[cid] = "Auth & Users"
        elif any('expert' in l.lower() and 'page' in l.lower() for l in node_labels):
            labels[cid] = "Expert Portal"
        elif any('expert' in l.lower() and 'card' in l.lower() for l in node_labels):
            labels[cid] = "Expert Directory"
        else:
            labels[cid] = f"Community {cid}"

    # Regenerate questions with real labels
    questions = suggest_questions(G, communities, labels)

    report = generate(G, communities, cohesion, labels, analysis['gods'], analysis['surprises'], detection, tokens, '.', suggested_questions=questions)
    Path('graphify-out/GRAPH_REPORT.md').write_text(report, encoding='utf-8')
    Path('graphify-out/.graphify_labels.json').write_text(json.dumps({str(k): v for k, v in labels.items()}))
    print('Communities labeled')
    for cid, label in sorted(labels.items()):
        print(f'  {cid}: {label}')
