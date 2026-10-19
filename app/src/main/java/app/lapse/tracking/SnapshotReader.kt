package app.lapse.tracking

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import app.lapse.engine.NodeSignal
import app.lapse.engine.ScrollSignal

/**
 * Copies the smallest set of screen facts the counter needs, then recycles every node.
 * Text is truncated and, for Reddit rows, reduced to a hash before it leaves this class.
 */
class SnapshotReader {
    fun read(root: AccessibilityNodeInfo?): List<NodeSignal> {
        if (root == null) return emptyList()
        val out = ArrayList<NodeSignal>(80)
        val pending = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        try {
            enqueueChildren(root, 0, out, pending)
            while (pending.isNotEmpty() && out.size < MAX_NODES) {
                val (node, depth) = pending.removeFirst()
                try {
                    enqueueChildren(node, depth, out, pending)
                } finally {
                    node.recycle()
                }
            }
        } catch (_: RuntimeException) {
            // The window can disappear mid-walk. Keep whatever was already copied.
        } finally {
            while (pending.isNotEmpty()) {
                pending.removeFirst().first.recycle()
            }
            root.recycle()
        }
        return out
    }

    fun readScroll(source: AccessibilityNodeInfo?, eventFromIndex: Int, eventToIndex: Int, itemCount: Int, scrollDeltaY: Int, includeKeys: Boolean): ScrollSignal? {
        if (source == null && eventFromIndex < 0 && scrollDeltaY == 0) return null
        return try {
            val bounds = Rect()
            source?.getBoundsInScreen(bounds)
            ScrollSignal(
                viewId = source?.viewIdResourceName,
                className = source?.className?.toString(),
                fromIndex = eventFromIndex,
                toIndex = eventToIndex,
                itemCount = itemCount,
                scrollDeltaY = scrollDeltaY,
                viewportHeight = bounds.height(),
                visibleItemKeys = if (includeKeys) childKeys(source) else emptyList(),
            )
        } catch (_: RuntimeException) {
            null
        } finally {
            source?.recycle()
        }
    }

    private fun enqueueChildren(
        node: AccessibilityNodeInfo,
        depth: Int,
        out: MutableList<NodeSignal>,
        pending: ArrayDeque<Pair<AccessibilityNodeInfo, Int>>,
    ) {
        if (out.size >= MAX_NODES) return
        out.add(signal(node))
        if (depth >= MAX_DEPTH) return
        for (index in 0 until node.childCount) {
            if (out.size + pending.size >= MAX_NODES) return
            val child = try {
                node.getChild(index)
            } catch (_: RuntimeException) {
                null
            } ?: continue
            pending.add(child to depth + 1)
        }
    }

    private fun signal(node: AccessibilityNodeInfo): NodeSignal {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return NodeSignal(
            viewId = node.viewIdResourceName,
            className = node.className?.toString(),
            description = node.contentDescription?.toString()?.take(160),
            text = node.text?.toString()?.take(160),
            visible = node.isVisibleToUser,
            width = bounds.width(),
            height = bounds.height(),
            top = bounds.top,
        )
    }

    private fun childKeys(source: AccessibilityNodeInfo?): List<String> {
        if (source == null) return emptyList()
        val keys = ArrayList<String>(8)
        val count = source.childCount.coerceAtMost(8)
        for (index in 0 until count) {
            val child = try {
                source.getChild(index)
            } catch (_: RuntimeException) {
                null
            } ?: continue
            try {
                val label = child.text?.toString()
                    ?: child.contentDescription?.toString()
                    ?: grandchildLabel(child)
                if (!label.isNullOrBlank()) {
                    keys.add(label.trim().lowercase().hashCode().toString())
                }
            } finally {
                child.recycle()
            }
        }
        return keys
    }

    private fun grandchildLabel(node: AccessibilityNodeInfo): String? {
        val count = node.childCount.coerceAtMost(4)
        for (index in 0 until count) {
            val child = node.getChild(index) ?: continue
            try {
                val text = child.text?.toString() ?: child.contentDescription?.toString()
                if (!text.isNullOrBlank()) return text
            } finally {
                child.recycle()
            }
        }
        return null
    }

    private companion object {
        const val MAX_NODES = 120
        const val MAX_DEPTH = 14
    }
}
