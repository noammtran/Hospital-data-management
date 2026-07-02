"""
Add two more flowchart-style class diagrams (db-wiring, lookup-dereference)
into Diagram.mdj as new UMLModels. Horizontal layout, labelled arrows.
Boxes = UMLClass views with suppressed compartments; arrows = UMLDependency.
"""
import json, sys

MDJ = "Diagram.mdj"
PROJECT_ID = "AAAAAAFF+h6SjaM2Hec="

def name_compartment(box_id, model_id, pfx, L, T, W, H, text):
    ncv_id = f"{pfx}_ncv"
    lbl_ids = [f"{pfx}_lblA", f"{pfx}_lblB", f"{pfx}_lblC", f"{pfx}_lblD"]
    labels = []
    for i, lid in enumerate(lbl_ids):
        lbl = {"_type": "LabelView", "_id": lid, "_parent": {"$ref": ncv_id},
               "font": "Arial;13;1" if i == 1 else "Arial;13;0",
               "parentStyle": True, "height": 13}
        if i == 1:
            lbl.update({"left": L + 10, "top": T + 10, "width": W - 20,
                        "text": text, "wordWrap": True})
        else:
            lbl.update({"visible": False, "left": L, "top": T, "width": 1})
        labels.append(lbl)
    return {"_type": "UMLNameCompartmentView", "_id": ncv_id,
            "_parent": {"$ref": box_id}, "model": {"$ref": model_id},
            "subViews": labels, "font": "Arial;13;0", "parentStyle": True,
            "left": L, "top": T, "width": W, "height": H,
            "stereotypeLabel": {"$ref": lbl_ids[0]},
            "nameLabel": {"$ref": lbl_ids[1]},
            "namespaceLabel": {"$ref": lbl_ids[2]},
            "propertyLabel": {"$ref": lbl_ids[3]}}

def hidden(t, box_id, model_id, cid, L, T):
    return {"_type": t, "_id": cid, "_parent": {"$ref": box_id},
            "model": {"$ref": model_id}, "visible": False,
            "font": "Arial;13;0", "parentStyle": True,
            "left": L, "top": T, "width": 10, "height": 10}

def class_view(box_id, model_id, pfx, dgm_id, L, T, W, H, text):
    ncv = name_compartment(box_id, model_id, pfx, L, T, W, H, text)
    acv = hidden("UMLAttributeCompartmentView", box_id, model_id, f"{pfx}_acv", L, T)
    ocv = hidden("UMLOperationCompartmentView", box_id, model_id, f"{pfx}_ocv", L, T)
    rcv = hidden("UMLReceptionCompartmentView", box_id, model_id, f"{pfx}_rcv", L, T)
    tcv = hidden("UMLTemplateParameterCompartmentView", box_id, model_id, f"{pfx}_tcv", L, T)
    return {"_type": "UMLClassView", "_id": box_id, "_parent": {"$ref": dgm_id},
            "model": {"$ref": model_id}, "subViews": [ncv, acv, ocv, rcv, tcv],
            "font": "Arial;13;0", "parentStyle": False, "containerChangeable": True,
            "left": L, "top": T, "width": W, "height": H,
            "nameCompartment": {"$ref": ncv["_id"]},
            "suppressAttributes": True, "suppressOperations": True,
            "attributeCompartment": {"$ref": acv["_id"]},
            "operationCompartment": {"$ref": ocv["_id"]},
            "receptionCompartment": {"$ref": rcv["_id"]},
            "templateParameterCompartment": {"$ref": tcv["_id"]}}

def dependency_view(dpv_id, model_id, dgm_id, tail_v, head_v, points, label, lx, ly):
    els = []
    for s in ("A", "B", "C"):
        e = {"_type": "EdgeLabelView", "_id": f"{dpv_id}_el{s}",
             "_parent": {"$ref": dpv_id}, "model": {"$ref": model_id},
             "lineColor": "#333333", "font": "Arial;13;0", "parentStyle": True,
             "width": 110, "height": 13, "alpha": 1.5708, "distance": 15, "edgePosition": 1}
        if s == "A" and label:
            e.update({"visible": True, "left": lx, "top": ly, "text": label, "wordWrap": True})
        else:
            e.update({"visible": False, "left": lx, "top": ly})
        els.append(e)
    return {"_type": "UMLDependencyView", "_id": dpv_id, "_parent": {"$ref": dgm_id},
            "model": {"$ref": model_id}, "subViews": els,
            "lineColor": "#333333", "font": "Arial;13;0", "parentStyle": False,
            "head": {"$ref": head_v}, "tail": {"$ref": tail_v},
            "lineStyle": 1, "points": points, "showVisibility": True,
            "nameLabel": {"$ref": els[0]["_id"]},
            "stereotypeLabel": {"$ref": els[1]["_id"]},
            "propertyLabel": {"$ref": els[2]["_id"]}}

def build_horizontal(model_name, diagram_name, steps, edge_labels, pfx,
                     W=220, H=90, gap=150, top=240, left0=60):
    model_id, dgm_id = f"{pfx}_mdl", f"{pfx}_dgm"
    classes, views, deps, dep_views = [], [], [], []
    box_ids, model_ids, lefts = [], [], []
    for i, text in enumerate(steps):
        L = left0 + i * (W + gap)
        cls_id, box_id = f"{pfx}_cls{i:02d}", f"{pfx}_clv{i:02d}"
        classes.append({"_type": "UMLClass", "_id": cls_id,
                        "_parent": {"$ref": model_id}, "name": text})
        views.append(class_view(box_id, cls_id, f"{pfx}{i:02d}", dgm_id, L, top, W, H, text))
        box_ids.append(box_id); model_ids.append(cls_id); lefts.append(L)
    cy = top + H // 2
    for i in range(len(steps) - 1):
        dep_id, dpv_id = f"{pfx}_dep{i:02d}", f"{pfx}_dpv{i:02d}"
        lbl = edge_labels[i] if edge_labels else None
        dep = {"_type": "UMLDependency", "_id": dep_id, "_parent": {"$ref": model_id},
               "source": {"$ref": model_ids[i]}, "target": {"$ref": model_ids[i+1]}}
        if lbl:
            dep["name"] = lbl  # StarUML draws the edge label from the model name
        deps.append(dep)
        tx, hx = lefts[i] + W, lefts[i+1]
        pts = f"{tx}:{cy};{hx}:{cy}"
        midx = (tx + hx) // 2
        dep_views.append(dependency_view(dpv_id, dep_id, dgm_id, box_ids[i], box_ids[i+1],
                                          pts, lbl, midx - 55, cy - 28))
    diagram = {"_type": "UMLClassDiagram", "_id": dgm_id, "_parent": {"$ref": model_id},
               "name": diagram_name, "ownedViews": views + dep_views}
    return {"_type": "UMLModel", "_id": model_id, "_parent": {"$ref": PROJECT_ID},
            "name": model_name, "ownedElements": [diagram] + classes + deps}

models = [
    build_horizontal(
        "Report - Database wiring", "db-wiring",
        ["Environment variables: address, username, password",
         "Hospital application",
         "Oracle database"],
        ["read at startup", "connects and sets up tables on first run"],
        "rptW"),
    build_horizontal(
        "Report - Lookup dereference", "lookup-dereference",
        ["Patient row: gender stored as 1",
         "Shared list of options: GENDER 1=Nam, 2=Nữ, 3=Khác",
         "UI displays: Nam"],
        ["look up by group + code", "return display text"],
        "rptL"),
]

with open(MDJ, "r", encoding="utf-8") as f:
    text = f.read()
anchor = '\n\t],\n\t"documentVersion"'
idx = text.rfind(anchor)
if idx == -1:
    print("ANCHOR NOT FOUND", file=sys.stderr); sys.exit(1)

blocks = []
for m in models:
    dumped = json.dumps(m, indent="\t", ensure_ascii=False)
    blocks.append("\n".join("\t\t" + ln for ln in dumped.split("\n")))
insertion = ",\n" + ",\n".join(blocks)
new_text = text[:idx] + insertion + text[idx:]
json.loads(new_text)  # validate
with open(MDJ, "w", encoding="utf-8") as f:
    f.write(new_text)
print("OK: inserted db-wiring + lookup-dereference; file still valid JSON.")
