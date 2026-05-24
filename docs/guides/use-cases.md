# Use Cases & Applications

DrawBox can be used in a wide variety of applications across different domains. Here are common use cases where DrawBox excels:

## Productivity & Annotation Tools

### Screenshot & Image Annotation
Perfect for creating markup tools where users can annotate screenshots, diagrams, or images with:
- Freehand pen strokes for highlighting
- Arrows for pointing out specific areas
- Shapes for framing important regions
- Color coding for different types of annotations

**Example**: Screenshot annotation app, feedback tools, design review platforms

### Document Markup
Enable users to annotate PDFs and documents with:
- Handwritten notes directly on documents
- Highlighting and underlining
- Drawing attention to specific sections
- Exporting marked-up documents as images

## Creative & Design Applications

### Digital Drawing Apps
Full-featured drawing applications with:
- Multiple brush modes (pen, shapes, lines, arrows)
- Color palette management
- Undo/redo history
- SVG export for vector graphics
- PNG export for raster sharing

**Platforms**: Android, iOS, Web, Desktop

### Mind Mapping & Diagramming
Create mind maps and technical diagrams with:
- Shape tools for creating boxes, circles, triangles
- Arrow tools for connecting elements
- Color coding for organization
- JSON export for saving complex structures

### Whiteboard & Collaboration
Real-time collaborative drawing with:
- Multiple drawing modes
- Shared canvas state via JSON
- Export capabilities for documentation
- Cross-platform support for various devices

## Educational Applications

### Interactive Learning Tools
Enhance educational apps with drawing capabilities:
- Math problem visualization
- Geometry and shape exploration
- Handwriting practice
- Diagram creation for science lessons

### E-Learning Platforms
Add drawing features for student interaction:
- Quiz annotations
- Homework submission with drawings
- Interactive problem-solving

## Business & Enterprise

### Form & Document Applications
Add drawing capabilities to business apps:
- Signature capture (using pen mode)
- Form annotation and approval
- Technical drawing for service requests
- Invoice markup for approvals

### CAD & Design Software
Use as a canvas layer for:
- Architectural sketching
- Technical drawing base
- Design preview and markup
- UI mockup annotation

## Social & Communication

### Messaging & Collaboration Apps
Enable users to express themselves visually:
- Drawing in chat messages
- Sketching ideas in real-time
- Sharing creative content
- Canvas-based communication

## Technical Advantages for These Use Cases

| Use Case | Key Features | Export Format |
|----------|--------------|---------------|
| Annotation Tools | Shapes, arrows, colors | PNG/SVG |
| Creative Apps | Full drawing suite, undo/redo | SVG/PNG/JSON |
| Whiteboard | Collaboration state, history | JSON/SVG |
| Education | Multiple shapes, colors | SVG/PNG |
| Business Forms | Signature capture, markup | PNG/SVG |

## Integration Patterns

### As a Modal/Dialog
```kotlin
@Composable
fun DrawingDialog(onSave: (String) -> Unit) {
    val controller = rememberDrawBoxController()
    
    Column {
        DrawBox(
            state = controller.state.collectAsState().value,
            onIntent = controller::onIntent,
            modifier = Modifier.fillMaxSize().weight(1f)
        )
        Button(onClick = {
            val svg = controller.exportSvg()
            onSave(svg)
        }) {
            Text("Save Drawing")
        }
    }
}
```

### As a Feature in Larger App
```kotlin
@Composable
fun DocumentAnnotationScreen(document: Document) {
    val controller = rememberDrawBoxController()
    
    Box {
        DocumentViewer(document = document)
        DrawBox(
            state = controller.state.collectAsState().value,
            onIntent = controller::onIntent,
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

## Why Choose DrawBox?

✅ **Multiplatform** - One codebase for Android, iOS, Web, Desktop  
✅ **Production-Ready** - Fully featured with undo/redo, multiple modes  
✅ **Export Formats** - SVG for vectors, PNG for raster, JSON for state preservation  
✅ **Easy Integration** - Simple Compose API with comprehensive documentation  
✅ **Modern Stack** - Built with Kotlin Multiplatform & Jetpack Compose  
✅ **Open Source** - Apache 2.0 licensed, community-driven development  

---

## What Users Are Building

DrawBox is used in various applications across the ecosystem. If you're building something interesting with DrawBox, [let us know](https://github.com/akshay2211/DrawBox/discussions)!