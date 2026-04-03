package com.tyron.completion.xml.v2.handler

import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.rendering.api.StyleableResourceValue
import com.tyron.builder.project.api.AndroidModule
import com.tyron.completion.CompletionParameters
import com.tyron.completion.xml.insert.NamespaceInsertHandler
import com.tyron.completion.model.CompletionItem
import com.tyron.completion.model.CompletionList
import com.tyron.completion.model.DrawableKind
import com.tyron.completion.xml.model.XmlCompletionType
import com.tyron.completion.xml.util.XmlUtils
import com.tyron.completion.xml.v2.aar.FrameworkResourceRepository
import com.tyron.completion.xml.v2.project.ResourceRepositoryManager
import org.eclipse.lemminx.dom.DOMAttr
import org.eclipse.lemminx.dom.DOMDocument
import org.eclipse.lemminx.dom.DOMElement
import org.eclipse.lemminx.dom.DOMParser
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager

fun handleDrawable(
    frameworkResRepository: FrameworkResourceRepository,
    params: CompletionParameters
): CompletionList? {
    val repositoryManager = ResourceRepositoryManager.getInstance(params.module as AndroidModule)
    val parsedNode = DOMParser.getInstance().parse(
        params.contents,
        repositoryManager.namespace.xmlNamespaceUri,
        URIResolverExtensionManager()
    )
    val completionType = XmlUtils.getCompletionType(parsedNode, params.index)
    if (completionType == XmlCompletionType.UNKNOWN) {
        return CompletionList.EMPTY
    }

    val prefix = XmlUtils.getPrefix(parsedNode, params.index, completionType) ?: ""
    val completionBuilder = CompletionList.builder(prefix)

    when (completionType) {
        XmlCompletionType.TAG -> {
            addDrawableTags(completionBuilder, parsedNode, params.index.toInt())
        }
        XmlCompletionType.ATTRIBUTE -> {
            val node = parsedNode.findNodeAt(params.index.toInt())
            if (node is DOMElement) {
                val prefix = completionBuilder.prefix
                if (prefix.startsWith("xmlns:")) {
                    val namespaces = listOf(
                        "xmlns:android=\"http://schemas.android.com/apk/res/android\"",
                        "xmlns:app=\"http://schemas.android.com/apk/res-auto\"",
                        "xmlns:tools=\"http://schemas.android.com/tools\""
                    )
                    for (ns in namespaces) {
                        val label = ns.substring(0, ns.indexOf('='))
                        val value = ns.substring(ns.indexOf('=') + 2, ns.length - 1)
                        if (label.startsWith(prefix)) {
                            val item = CompletionItem.create(label, "Namespace", label, DrawableKind.Attribute)
                            item.commitText = label
                            item.setInsertHandler(NamespaceInsertHandler(item))
                            item.data = value
                            completionBuilder.addItem(item)
                        }
                    }
                }
                addDrawableAttributes(completionBuilder, frameworkResRepository, repositoryManager, node)
            }
        }
        XmlCompletionType.ATTRIBUTE_VALUE -> {
            val attr = parsedNode.findAttrAt(params.index.toInt())
            if (attr != null) {
                addValueItems(
                    completionBuilder,
                    frameworkResRepository,
                    repositoryManager.appResources,
                    repositoryManager.namespace,
                    attr,
                    params
                ) { tagName ->
                    getDrawableStyleableNames(tagName, attr.ownerElement)
                }
            }
        }
        else -> {}
    }

    return completionBuilder.build()
}

private fun addDrawableTags(
    builder: CompletionList.Builder,
    document: DOMDocument,
    index: Int
) {
    val node = document.findNodeAt(index)
    val parent = if (node is DOMElement) {
        if (index <= node.start + node.tagName.length + 1) node.parentElement else node
    } else {
        null
    }

    val tags = when (parent?.tagName) {
        null -> listOf(
            "shape", "selector", "layer-list", "vector", "animated-vector",
            "animated-selector", "animation-list", "inset", "clip", "scale",
            "rotate", "transition", "level-list", "ripple", "bitmap", "nine-patch",
            "color", "objectAnimator", "animated-image-train"
        )
        "shape" -> listOf("solid", "stroke", "corners", "padding", "gradient", "size")
        "selector", "animated-selector" -> listOf("item")
        "layer-list", "level-list", "animation-list", "transition" -> listOf("item")
        "vector", "animated-vector" -> listOf("path", "group", "clip-path")
        "group" -> listOf("path", "group", "clip-path")
        "item" -> {
            listOf("shape", "selector", "layer-list", "vector", "animated-vector", "bitmap", "nine-patch", "inset", "clip", "scale", "rotate", "ripple")
        }
        else -> emptyList()
    }

    tags.forEach { tag ->
        val item = CompletionItem().apply {
            label = tag
            desc = "Tag"
            iconKind = DrawableKind.Class
            commitText = tag
            addFilterText(tag)
        }
        builder.addItem(item)
    }
}

private fun addDrawableAttributes(
    builder: CompletionList.Builder,
    frameworkResRepository: FrameworkResourceRepository,
    repositoryManager: ResourceRepositoryManager,
    node: DOMElement
) {
    val styleNames = getDrawableStyleableNames(node.tagName, node)

    val items = styleNames.flatMap { styleName ->
        getStyleables(
            listOf(ResourceNamespace.RES_AUTO.xmlNamespaceUri),
            frameworkResRepository,
            repositoryManager.appResources
        ) {
            it.name == styleName
        }
    }.filterIsInstance<StyleableResourceValue>()
    .flatMap { it.allAttributes }

    addAttributes(items, node, builder)
}

private fun getDrawableStyleableNames(tagName: String, node: DOMElement?): List<String> {
    return when (tagName) {
        "shape" -> listOf("GradientDrawable")
        "solid" -> listOf("GradientDrawableSolid")
        "stroke" -> listOf("GradientDrawableStroke")
        "corners" -> listOf("GradientDrawableCorners")
        "padding" -> listOf("GradientDrawablePadding")
        "gradient" -> listOf("GradientDrawableGradient")
        "size" -> listOf("GradientDrawableSize")
        "selector" -> listOf("StateListDrawable")
        "animated-selector" -> listOf("AnimatedStateListDrawable")
        "item" -> {
            val parent = node?.parentElement
            when (parent?.tagName) {
                "selector", "animated-selector" -> listOf("StateListDrawableItem")
                "layer-list" -> listOf("LayerDrawableItem")
                "ripple" -> listOf("RippleDrawableItem")
                "level-list" -> listOf("LevelListDrawableItem")
                "animation-list" -> listOf("AnimationDrawableItem")
                else -> listOf("LayerDrawableItem")
            }
        }
        "vector" -> listOf("VectorDrawable")
        "path" -> listOf("VectorDrawablePath")
        "group" -> listOf("VectorDrawableGroup")
        "clip-path" -> listOf("VectorDrawableClipPath")
        "animated-vector" -> listOf("AnimatedVectorDrawable")
        "ripple" -> listOf("RippleDrawable")
        "inset" -> listOf("InsetDrawable")
        "clip" -> listOf("ClipDrawable")
        "scale" -> listOf("ScaleDrawable")
        "rotate" -> listOf("RotateDrawable")
        "animation-list" -> listOf("AnimationDrawable")
        "bitmap" -> listOf("BitmapDrawable")
        "nine-patch" -> listOf("NinePatchDrawable")
        "color" -> listOf("ColorDrawable")
        "transition" -> listOf("TransitionDrawable")
        else -> emptyList()
    }
}
