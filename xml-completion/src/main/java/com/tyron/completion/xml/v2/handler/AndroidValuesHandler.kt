package com.tyron.completion.xml.v2.handler

import com.android.ide.common.rendering.api.AttrResourceValue
import com.android.ide.common.rendering.api.AttributeFormat
import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.resources.ResourceType
import com.tyron.builder.project.api.AndroidModule
import com.tyron.completion.CompletionParameters
import com.tyron.completion.model.CompletionItem
import com.tyron.completion.model.CompletionList
import com.tyron.completion.model.DrawableKind
import com.tyron.completion.xml.insert.NamespaceInsertHandler
import com.tyron.completion.xml.model.XmlCompletionType
import com.tyron.completion.xml.util.XmlUtils
import com.tyron.completion.xml.v2.aar.FrameworkResourceRepository
import com.tyron.completion.xml.v2.project.ResourceRepositoryManager
import org.eclipse.lemminx.dom.DOMAttr
import org.eclipse.lemminx.dom.DOMDocument
import org.eclipse.lemminx.dom.DOMElement
import org.eclipse.lemminx.dom.DOMParser
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager

fun handleValues(
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

    var prefix = XmlUtils.getPrefix(parsedNode, params.index, completionType)
    
    if (prefix == null) {
        prefix = getManualPrefix(params.contents, params.index.toInt())
    }

    val completionBuilder = CompletionList.builder(prefix)

    when (completionType) {
        XmlCompletionType.TAG -> {
            addValuesTags(completionBuilder, parsedNode, params.index.toInt())
            XmlSnippetHandler.addSnippets(completionBuilder, XmlSnippetScope.TAG, prefix)
        }
        XmlCompletionType.ATTRIBUTE -> {
            val node = parsedNode.findNodeAt(params.index.toInt())
            if (node is DOMElement) {
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

                val text = parsedNode.text
                val startTagOpen = node.start
                val startTagClosingBracket = text.indexOf('>', startTagOpen)
                
                if (startTagClosingBracket != -1 && params.index <= startTagClosingBracket) {
                    addValuesAttributes(completionBuilder, node)
                    XmlSnippetHandler.addSnippets(completionBuilder, XmlSnippetScope.ATTRIBUTE, prefix)
                } else {
                    addValuesTextContent(completionBuilder, frameworkResRepository, repositoryManager, node, prefix)
                    XmlSnippetHandler.addSnippets(completionBuilder, XmlSnippetScope.VALUE, prefix)
                }
            }
        }
        XmlCompletionType.ATTRIBUTE_VALUE -> {
            val attr = parsedNode.findAttrAt(params.index.toInt())
            if (attr != null) {
                addValuesAttributeValues(
                    completionBuilder,
                    frameworkResRepository,
                    repositoryManager,
                    attr,
                    params
                )
                XmlSnippetHandler.addSnippets(completionBuilder, XmlSnippetScope.VALUE, prefix)
            }
        }
        else -> {}
    }

    return completionBuilder.build()
}

private fun getManualPrefix(contents: String, index: Int): String {
    var start = index
    while (start > 0) {
        val c = contents[start - 1]
        if (c == '>' || c == '<' || c == '"' || c == '\'' || c == ' ' || c == '\n' || c == '\t') {
            break
        }
        start--
    }
    return contents.substring(start, index)
}

private fun addValuesTags(
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
    
    val parentName = parent?.tagName

    val tags = when (parentName) {
        null -> listOf("resources")
        "resources" -> listOf(
            "style",
            "declare-styleable",
            "attr",
            "color",
            "dimen",
            "string",
            "integer",
            "bool",
            "array",
            "integer-array",
            "string-array",
            "item",
            "public"
        )
        "style" -> listOf("item")
        "declare-styleable" -> listOf("attr")
        "attr" -> listOf("enum", "flag")
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

private fun addValuesAttributes(
    builder: CompletionList.Builder,
    node: DOMElement
) {
    val attributes = when (node.tagName) {
        "style" -> listOf("name", "parent")
        "item" -> listOf("name", "type", "format")
        "declare-styleable" -> listOf("name")
        "attr" -> listOf("name", "format")
        "enum", "flag" -> listOf("name", "value")
        "color", "string", "dimen", "integer", "bool" -> listOf("name")
        else -> emptyList()
    }

    attributes.forEach { attr ->
        val item = CompletionItem().apply {
            label = attr
            desc = "Attribute"
            iconKind = DrawableKind.Attribute
            commitText = "$attr=\"\""
            cursorOffset = commitText.length - 1
            addFilterText(attr)
        }
        builder.addItem(item)
    }
}

private fun addValuesAttributeValues(
    builder: CompletionList.Builder,
    frameworkResRepository: FrameworkResourceRepository,
    repositoryManager: ResourceRepositoryManager,
    attr: DOMAttr,
    params: CompletionParameters
) {
    val ownerElement = attr.ownerElement ?: return
    val tagName = ownerElement.tagName
    val attrName = attr.localName

    when {
        tagName == "item" && attrName == "name" && ownerElement.parentElement?.tagName == "style" -> {
            val projectResources = repositoryManager.appResources
            val namespace = repositoryManager.namespace

            val attributes = frameworkResRepository.getPublicResources(
                ResourceNamespace.ANDROID,
                ResourceType.ATTR
            ) + projectResources.getResources(namespace, ResourceType.ATTR).values()

            attributes.forEach { resItem ->
                val name = resItem.name
                val isAndroid = resItem.namespace == ResourceNamespace.ANDROID
                val label = if (isAndroid) "android:$name" else name
                val item = CompletionItem().apply {
                    this.label = label
                    this.desc = "Attribute"
                    this.iconKind = DrawableKind.Attribute
                    this.commitText = label
                    addFilterText(label)
                    addFilterText(name)
                }
                builder.addItem(item)
            }
        }
        tagName == "style" && attrName == "parent" -> {
            val projectResources = repositoryManager.appResources
            val namespace = repositoryManager.namespace

            val styles = frameworkResRepository.getPublicResources(
                ResourceNamespace.ANDROID,
                ResourceType.STYLE
            ) + projectResources.getResources(namespace, ResourceType.STYLE).values()

            styles.forEach { resItem ->
                val name = resItem.name
                val isAndroid = resItem.namespace == ResourceNamespace.ANDROID
                val label = if (isAndroid) "@android:style/$name" else name
                val item = CompletionItem().apply {
                    this.label = label
                    this.desc = "Style"
                    this.iconKind = DrawableKind.Class
                    this.commitText = label
                    addFilterText(label)
                    addFilterText(name)
                }
                builder.addItem(item)
            }
        }
        (tagName == "attr" || tagName == "item") && attrName == "format" -> {
            val formats = listOf(
                "reference", "string", "color", "dimension", "boolean", "integer", "fraction", "enum", "flag", "float"
            )
            formats.forEach { format ->
                val item = CompletionItem().apply {
                    this.label = format
                    this.desc = "Format"
                    this.iconKind = DrawableKind.Snippet
                    this.commitText = format
                    addFilterText(format)
                }
                builder.addItem(item)
            }
        }
    }
}

private fun addValuesTextContent(
    builder: CompletionList.Builder,
    frameworkResRepository: FrameworkResourceRepository,
    repositoryManager: ResourceRepositoryManager,
    node: DOMElement,
    prefix: String
) {
    if (node.tagName == "item" && node.parentElement?.tagName == "style") {
        val attrName = node.getAttribute("name")
        if (attrName.isNotEmpty()) {
            val (ns, localName) = if (attrName.contains(":")) {
                val p = attrName.substringBefore(":")
                val n = attrName.substringAfter(":")
                val namespace = if (p == "android") ResourceNamespace.ANDROID else repositoryManager.namespace
                namespace to n
            } else {
                repositoryManager.namespace to attrName
            }

            val attrResources = (frameworkResRepository.getResources(ns, ResourceType.ATTR, localName) +
                                repositoryManager.appResources.getResources(ns, ResourceType.ATTR, localName))
            
            val attrRes = attrResources.firstOrNull()?.resourceValue as? AttrResourceValue
            if (attrRes != null) {
                attrRes.formats.forEach { format ->
                    when (format) {
                        AttributeFormat.BOOLEAN -> {
                             listOf("true", "false").forEach { valStr ->
                                 builder.addItem(CompletionItem().apply {
                                     label = valStr
                                     desc = "Boolean"
                                     iconKind = DrawableKind.Snippet
                                     commitText = valStr
                                     addFilterText(valStr)
                                 })
                             }
                        }
                        AttributeFormat.ENUM, AttributeFormat.FLAGS -> {
                            attrRes.attributeValues.keys.forEach { valStr ->
                                builder.addItem(CompletionItem().apply {
                                    label = valStr
                                    desc = "Value"
                                    iconKind = DrawableKind.Snippet
                                    commitText = valStr
                                    addFilterText(valStr)
                                })
                            }
                        }
                        else -> {
                            suggestResourceReferences(builder, frameworkResRepository, repositoryManager, prefix, format)
                        }
                    }
                }
                return
            }
        }
    }
    
    val tagType = com.android.resources.ResourceType.fromXmlTagName(node.tagName)
    if (tagType != null) {
        val format = when (tagType) {
            com.android.resources.ResourceType.COLOR -> AttributeFormat.COLOR
            com.android.resources.ResourceType.DIMEN -> AttributeFormat.DIMENSION
            com.android.resources.ResourceType.STRING -> AttributeFormat.STRING
            com.android.resources.ResourceType.INTEGER -> AttributeFormat.INTEGER
            com.android.resources.ResourceType.BOOL -> AttributeFormat.BOOLEAN
            else -> AttributeFormat.REFERENCE
        }
        suggestResourceReferences(builder, frameworkResRepository, repositoryManager, prefix, format)
    }
}

private fun suggestResourceReferences(
    builder: CompletionList.Builder,
    frameworkResRepository: FrameworkResourceRepository,
    repositoryManager: ResourceRepositoryManager,
    prefix: String,
    format: AttributeFormat
) {
    val resourceType = when (format) {
        AttributeFormat.COLOR -> ResourceType.COLOR
        AttributeFormat.DIMENSION -> ResourceType.DIMEN
        AttributeFormat.STRING -> ResourceType.STRING
        AttributeFormat.INTEGER -> ResourceType.INTEGER
        AttributeFormat.BOOLEAN -> ResourceType.BOOL
        else -> null
    }
    
    val types = if (resourceType != null) listOf(resourceType) else ResourceType.REFERENCEABLE_TYPES
    
    types.forEach { type ->
        val resItems = (frameworkResRepository.getResources(ResourceNamespace.ANDROID, type).values() +
                       repositoryManager.appResources.getResources(repositoryManager.namespace, type).values())
        
        resItems.forEach { resItem ->
            val resName = resItem.name
            val isAndroid = resItem.namespace == ResourceNamespace.ANDROID
            val url = "@${if (isAndroid) "android:" else ""}${type.getName()}/$resName"
            
            if (url.startsWith(prefix) || resName.startsWith(prefix) || (prefix.startsWith("@") && url.startsWith(prefix)) || (prefix.startsWith("?") && url.replace("@", "?").startsWith(prefix))) {
                builder.addItem(CompletionItem().apply {
                    label = url
                    desc = type.displayName
                    iconKind = DrawableKind.Attribute
                    commitText = url
                    addFilterText(url)
                    addFilterText(resName)
                })
                
                if (format == AttributeFormat.REFERENCE) {
                    val themeUrl = url.replace("@", "?")
                    builder.addItem(CompletionItem().apply {
                        label = themeUrl
                        desc = type.displayName
                        iconKind = DrawableKind.Attribute
                        commitText = themeUrl
                        addFilterText(themeUrl)
                        addFilterText(resName)
                    })
                }
            }
        }
    }
}
