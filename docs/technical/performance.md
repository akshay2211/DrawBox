# Performance

DrawBox is optimized for performance across all platforms.

## Performance Tips

### Memory

- Clear drawings periodically with `reset()`
- Avoid very large stroke widths
- Limit element count on low-end devices

### Rendering

- Hardware acceleration enabled by default
- Efficient canvas drawing
- Optimized path algorithms

### Battery

- Minimal CPU usage when idle
- Efficient rendering pipeline
- Optimized for mobile devices

## Benchmarks

Performance varies by device and drawing complexity. On modern devices:

- **Canvas rendering**: 60 FPS
- **Undo/Redo**: Instant (<50ms)
- **SVG export**: <500ms for typical drawings
- **PNG export**: <1s for typical drawings

---

Contact author for detailed benchmarks or profiling help.
