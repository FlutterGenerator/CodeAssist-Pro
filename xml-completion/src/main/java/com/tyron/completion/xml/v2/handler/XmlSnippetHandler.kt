package com.tyron.completion.xml.v2.handler

import com.itsaky.androidide.lsp.snippets.DefaultSnippet
import com.itsaky.androidide.lsp.snippets.ISnippet
import com.tyron.completion.model.CompletionItem
import com.tyron.completion.model.CompletionList
import com.tyron.completion.model.DrawableKind
import com.tyron.completion.model.SnippetCompletionItem
import com.tyron.completion.xml.insert.DefaultXmlInsertHandler
import com.tyron.completion.xml.insert.LayoutTagInsertHandler
import io.github.rosemoe.sora.lang.completion.SnippetDescription
import io.github.rosemoe.sora.lang.completion.snippet.parser.CodeSnippetParser

object XmlSnippetHandler {

    private val MANUAL_SNIPPETS = mapOf(
        XmlSnippetScope.TAG to listOf(
            DefaultSnippet("comm", "Create an XML comment", arrayOf("<!-- \$0 -->")),
            DefaultSnippet("todo", "Create a TODO comment", arrayOf("<!-- TODO: \$0 -->")),
            DefaultSnippet("cdata", "Create a CDATA section", arrayOf("<![CDATA[\$0]]>")),
            DefaultSnippet("include", "Include a layout", arrayOf("<include layout=\"@layout/\${1:layout_name}\" />")),
            DefaultSnippet("merge", "Merge tag", arrayOf("<merge>", "\t\$0", "</merge>")),
            DefaultSnippet("view", "Generic View tag", arrayOf("<View", "\tandroid:layout_width=\"\${1:match_parent}\"", "\tandroid:layout_height=\"\${2:wrap_content}\"\$0 />")),
            DefaultSnippet("text", "TextView", arrayOf("<TextView", "\tandroid:id=\"@+id/\${1:textView}\"", "\tandroid:layout_width=\"\${2:wrap_content}\"", "\tandroid:layout_height=\"\${3:wrap_content}\"", "\tandroid:text=\"\${4:Text}\"\$0 />")),
            DefaultSnippet("button", "Button", arrayOf("<Button", "\tandroid:id=\"@+id/\${1:button}\"", "\tandroid:layout_width=\"\${2:wrap_content}\"", "\tandroid:layout_height=\"\${3:wrap_content}\"", "\tandroid:text=\"\${4:Button}\"\$0 />")),
            DefaultSnippet("edit", "EditText", arrayOf("<EditText", "\tandroid:id=\"@+id/\${1:editText}\"", "\tandroid:layout_width=\"\${2:match_parent}\"", "\tandroid:layout_height=\"\${3:wrap_content}\"", "\tandroid:hint=\"\${4:Hint}\"\$0 />")),
            DefaultSnippet("image", "ImageView", arrayOf("<ImageView", "\tandroid:id=\"@+id/\${1:imageView}\"", "\tandroid:layout_width=\"\${2:wrap_content}\"", "\tandroid:layout_height=\"\${3:wrap_content}\"", "\tandroid:src=\"@drawable/\${4:src}\"\$0 />")),
            DefaultSnippet("imgbtn", "ImageButton", arrayOf("<ImageButton", "\tandroid:id=\"@+id/\${1:imageButton}\"", "\tandroid:layout_width=\"\${2:wrap_content}\"", "\tandroid:layout_height=\"\${3:wrap_content}\"", "\tandroid:src=\"@drawable/\${4:src}\"\$0 />")),
            DefaultSnippet("linear", "LinearLayout", arrayOf("<LinearLayout", "\tandroid:layout_width=\"\${1:match_parent}\"", "\tandroid:layout_height=\"\${2:match_parent}\"", "\tandroid:orientation=\"\${3:vertical}\">", "\t\$0", "</LinearLayout>")),
            DefaultSnippet("relative", "RelativeLayout", arrayOf("<RelativeLayout", "\tandroid:layout_width=\"\${1:match_parent}\"", "\tandroid:layout_height=\"\${2:match_parent}\">", "\t\$0", "</RelativeLayout>")),
            DefaultSnippet("frame", "FrameLayout", arrayOf("<FrameLayout", "\tandroid:layout_width=\"\${1:match_parent}\"", "\tandroid:layout_height=\"\${2:match_parent}\">", "\t\$0", "</FrameLayout>")),
            DefaultSnippet("scroll", "ScrollView", arrayOf("<ScrollView", "\tandroid:layout_width=\"\${1:match_parent}\"", "\tandroid:layout_height=\"\${2:match_parent}\">", "\t\$0", "</ScrollView>")),
            DefaultSnippet("nested", "NestedScrollView", arrayOf("<androidx.core.widget.NestedScrollView", "\tandroid:layout_width=\"\${1:match_parent}\"", "\tandroid:layout_height=\"\${2:match_parent}\"", "\tandroid:fillViewport=\"true\">", "\t\$0", "</androidx.core.widget.NestedScrollView>")),
            DefaultSnippet("constraint", "ConstraintLayout", arrayOf("<androidx.constraintlayout.widget.ConstraintLayout", "\tandroid:layout_width=\"\${1:match_parent}\"", "\tandroid:layout_height=\"\${2:match_parent}\">", "\t\$0", "</androidx.constraintlayout.widget.ConstraintLayout>")),
            DefaultSnippet("recycler", "RecyclerView", arrayOf("<androidx.recyclerview.widget.RecyclerView", "\tandroid:id=\"@+id/\${1:recyclerView}\"", "\tandroid:layout_width=\"\${2:match_parent}\"", "\tandroid:layout_height=\"\${3:match_parent}\"", "\tapp:layoutManager=\"\${4:LinearLayoutManager}\"\$0 />")),
            DefaultSnippet("card", "CardView", arrayOf("<androidx.cardview.widget.CardView", "\tandroid:layout_width=\"\${1:match_parent}\"", "\tandroid:layout_height=\"\${2:wrap_content}\"", "\tapp:cardCornerRadius=\"\${3:8dp}\">", "\t\$0", "</androidx.cardview.widget.CardView>")),
            DefaultSnippet("toolbar", "Toolbar", arrayOf("<androidx.appcompat.widget.Toolbar", "\tandroid:id=\"@+id/\${1:toolbar}\"", "\tandroid:layout_width=\"\${2:match_parent}\"", "\tandroid:layout_height=\"?attr/actionBarSize\"", "\tandroid:background=\"?attr/colorPrimary\"\$0 />")),
            DefaultSnippet("progress", "ProgressBar", arrayOf("<ProgressBar", "\tandroid:id=\"@+id/\${1:progressBar}\"", "\tandroid:layout_width=\"\${2:wrap_content}\"", "\tandroid:layout_height=\"\${3:wrap_content}\"\$0 />")),
            DefaultSnippet("swipe", "SwipeRefreshLayout", arrayOf("<androidx.swiperefreshlayout.widget.SwipeRefreshLayout", "\tandroid:id=\"@+id/\${1:swipeRefresh}\"", "\tandroid:layout_width=\"\${2:match_parent}\"", "\tandroid:layout_height=\"\${3:match_parent}\">", "\t\$0", "</androidx.swiperefreshlayout.widget.SwipeRefreshLayout>")),
            DefaultSnippet("checkbox", "CheckBox", arrayOf("<CheckBox", "\tandroid:id=\"@+id/\${1:checkBox}\"", "\tandroid:layout_width=\"\${2:wrap_content}\"", "\tandroid:layout_height=\"\${3:wrap_content}\"", "\tandroid:text=\"\${4:CheckBox}\"\$0 />")),
            DefaultSnippet("radio", "RadioButton", arrayOf("<RadioButton", "\tandroid:id=\"@+id/\${1:radioButton}\"", "\tandroid:layout_width=\"\${2:wrap_content}\"", "\tandroid:layout_height=\"\${3:wrap_content}\"", "\tandroid:text=\"\${4:RadioButton}\"\$0 />")),
            DefaultSnippet("switch", "MaterialSwitch", arrayOf("<com.google.android.material.materialswitch.MaterialSwitch", "\tandroid:id=\"@+id/\${1:materialSwitch}\"", "\tandroid:layout_width=\"\${2:wrap_content}\"", "\tandroid:layout_height=\"\${3:wrap_content}\"", "\tandroid:text=\"\${4:Switch}\"\$0 />")),
            DefaultSnippet("fab", "FloatingActionButton", arrayOf("<com.google.android.material.floatingactionbutton.FloatingActionButton", "\tandroid:id=\"@+id/\${1:fab}\"", "\tandroid:layout_width=\"wrap_content\"", "\tandroid:layout_height=\"wrap_content\"", "\tandroid:layout_gravity=\"bottom|end\"", "\tandroid:layout_margin=\"16dp\"", "\tapp:srcCompat=\"@android:drawable/\${2:ic_dialog_email}\"\$0 />")),
            DefaultSnippet("input", "TextInputLayout", arrayOf("<com.google.android.material.textfield.TextInputLayout", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"wrap_content\"", "\tandroid:hint=\"\${1:Hint}\">", "", "\t<com.google.android.material.textfield.TextInputEditText", "\t\tandroid:layout_width=\"match_parent\"", "\t\tandroid:layout_height=\"wrap_content\" />", "", "</com.google.android.material.textfield.TextInputLayout>")),
            DefaultSnippet("coord", "CoordinatorLayout", arrayOf("<androidx.coordinatorlayout.widget.CoordinatorLayout", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"match_parent\">", "\t\$0", "</androidx.coordinatorlayout.widget.CoordinatorLayout>")),
            DefaultSnippet("appbar", "AppBarLayout", arrayOf("<com.google.android.material.appbar.AppBarLayout", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"wrap_content\">", "\t\$0", "</com.google.android.material.appbar.AppBarLayout>")),
            DefaultSnippet("collapsing", "CollapsingToolbarLayout", arrayOf("<com.google.android.material.appbar.CollapsingToolbarLayout", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"match_parent\"", "\tapp:layout_scrollFlags=\"scroll|exitUntilCollapsed\"", "\tapp:contentScrim=\"?attr/colorPrimary\">", "\t\$0", "</com.google.android.material.appbar.CollapsingToolbarLayout>")),
            DefaultSnippet("bottomnav", "BottomNavigationView", arrayOf("<com.google.android.material.bottomnavigation.BottomNavigationView", "\tandroid:id=\"@+id/\${1:bottomNavigation}\"", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"wrap_content\"", "\tandroid:layout_gravity=\"bottom\"", "\tapp:menu=\"@menu/\${2:menu_name}\"\$0 />")),
            DefaultSnippet("tablayout", "TabLayout", arrayOf("<com.google.android.material.tabs.TabLayout", "\tandroid:id=\"@+id/\${1:tabLayout}\"", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"wrap_content\"\$0 />")),
            DefaultSnippet("vp2", "ViewPager2", arrayOf("<androidx.viewpager2.widget.ViewPager2", "\tandroid:id=\"@+id/\${1:viewPager2}\"", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"match_parent\"\$0 />")),
            DefaultSnippet("mbtn", "MaterialButton", arrayOf("<com.google.android.material.button.MaterialButton", "\tandroid:id=\"@+id/\${1:button}\"", "\tandroid:layout_width=\"wrap_content\"", "\tandroid:layout_height=\"wrap_content\"", "\tandroid:text=\"\${2:Button}\"\$0 />")),
            DefaultSnippet("mcard", "MaterialCardView", arrayOf("<com.google.android.material.card.MaterialCardView", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"wrap_content\"", "\tandroid:layout_margin=\"8dp\"", "\tapp:cardCornerRadius=\"8dp\">", "\t\$0", "</com.google.android.material.card.MaterialCardView>")),
            DefaultSnippet("mtoolbar", "MaterialToolbar", arrayOf("<com.google.android.material.appbar.MaterialToolbar", "\tandroid:id=\"@+id/\${1:toolbar}\"", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"?attr/actionBarSize\"", "\tandroid:background=\"?attr/colorPrimary\"", "\tapp:title=\"\${2:Title}\"\$0 />")),
            DefaultSnippet("chipgroup", "ChipGroup", arrayOf("<com.google.android.material.chip.ChipGroup", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"wrap_content\">", "\t\$0", "</com.google.android.material.chip.ChipGroup>")),
            DefaultSnippet("chip", "Chip", arrayOf("<com.google.android.material.chip.Chip", "\tandroid:layout_width=\"wrap_content\"", "\tandroid:layout_height=\"wrap_content\"", "\tandroid:text=\"\${1:Chip}\"\$0 />")),
            DefaultSnippet("efab", "ExtendedFloatingActionButton", arrayOf("<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton", "\tandroid:id=\"@+id/\${1:efab}\"", "\tandroid:layout_width=\"wrap_content\"", "\tandroid:layout_height=\"wrap_content\"", "\tandroid:layout_gravity=\"bottom|end\"", "\tandroid:layout_margin=\"16dp\"", "\tandroid:text=\"\${2:Label}\"", "\tapp:icon=\"@drawable/\${3:ic_add}\"\$0 />")),
            DefaultSnippet("drawer", "DrawerLayout", arrayOf("<androidx.drawerlayout.widget.DrawerLayout", "\tandroid:id=\"@+id/\${1:drawer_layout}\"", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"match_parent\">", "", "\t<include layout=\"@layout/\${2:content_main}\" />", "", "\t<com.google.android.material.navigation.NavigationView", "\t\tandroid:id=\"@+id/\${3:nav_view}\"", "\t\tandroid:layout_width=\"wrap_content\"", "\t\tandroid:layout_height=\"match_parent\"", "\t\tandroid:layout_gravity=\"start\"", "\t\tapp:headerLayout=\"@layout/\${4:nav_header}\"", "\t\tapp:menu=\"@menu/\${5:activity_main_drawer}\" />", "", "</androidx.drawerlayout.widget.DrawerLayout>")),
            DefaultSnippet("bottomsheet", "BottomSheet", arrayOf("<com.google.android.material.card.MaterialCardView", "\tandroid:id=\"@+id/\${1:bottom_sheet}\"", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"wrap_content\"", "\tapp:layout_behavior=\"com.google.android.material.bottomsheet.BottomSheetBehavior\">", "\t\$0", "</com.google.android.material.card.MaterialCardView>")),
            DefaultSnippet("motion", "MotionLayout", arrayOf("<androidx.constraintlayout.motion.widget.MotionLayout", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"match_parent\"", "\tapp:layoutDescription=\"@xml/\${1:motion_scene}\">", "\t\$0", "</androidx.constraintlayout.motion.widget.MotionLayout>")),
            DefaultSnippet("guideline", "Guideline", arrayOf("<androidx.constraintlayout.widget.Guideline", "\tandroid:id=\"@+id/\${1:guideline}\"", "\tandroid:layout_width=\"wrap_content\"", "\tandroid:layout_height=\"wrap_content\"", "\tandroid:orientation=\"\${2:vertical}\"", "\tapp:layout_constraintGuide_begin=\"\${3:16dp}\" />")),
            DefaultSnippet("barrier", "Barrier", arrayOf("<androidx.constraintlayout.widget.Barrier", "\tandroid:id=\"@+id/\${1:barrier}\"", "\tandroid:layout_width=\"wrap_content\"", "\tandroid:layout_height=\"wrap_content\"", "\tapp:barrierDirection=\"\${2:end}\"", "\tapp:constraint_referenced_ids=\"\${3:id1,id2}\" />")),
            DefaultSnippet("bottomappbar", "BottomAppBar", arrayOf("<com.google.android.material.bottomappbar.BottomAppBar", "\tandroid:id=\"@+id/\${1:bottomAppBar}\"", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"wrap_content\"", "\tandroid:layout_gravity=\"bottom\"", "\tapp:menu=\"@menu/\${2:menu_name}\" />")),
            DefaultSnippet("slider", "Slider", arrayOf("<com.google.android.material.slider.Slider", "\tandroid:id=\"@+id/\${1:slider}\"", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"wrap_content\"", "\tandroid:valueFrom=\"0.0\"", "\tandroid:valueTo=\"100.0\"\$0 />")),
            DefaultSnippet("divider", "MaterialDivider", arrayOf("<com.google.android.material.divider.MaterialDivider", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"wrap_content\"\$0 />")),
            DefaultSnippet("searchbar", "SearchBar", arrayOf("<com.google.android.material.search.SearchBar", "\tandroid:id=\"@+id/\${1:search_bar}\"", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"wrap_content\"", "\tandroid:hint=\"\${2:Search}\"\$0 />")),
            DefaultSnippet("searchview", "SearchView", arrayOf("<com.google.android.material.search.SearchView", "\tandroid:id=\"@+id/\${1:search_view}\"", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"match_parent\"", "\tandroid:hint=\"\${2:Search}\"\$0 />")),
            DefaultSnippet("draghandle", "BottomSheetDragHandleView", arrayOf("<com.google.android.material.bottomsheet.BottomSheetDragHandleView", "\tandroid:layout_width=\"match_parent\"", "\tandroid:layout_height=\"wrap_content\"\$0 />")),
            DefaultSnippet("clgroup", "Constraint Group", arrayOf("<androidx.constraintlayout.widget.Group", "\tandroid:id=\"@+id/\${1:group}\"", "\tandroid:layout_width=\"wrap_content\"", "\tandroid:layout_height=\"wrap_content\"", "\tapp:constraint_referenced_ids=\"\${2:id1,id2}\"\$0 />")),
            DefaultSnippet("clflow", "Constraint Flow", arrayOf("<androidx.constraintlayout.helper.widget.Flow", "\tandroid:id=\"@+id/\${1:flow}\"", "\tandroid:layout_width=\"0dp\"", "\tandroid:layout_height=\"wrap_content\"", "\tapp:constraint_referenced_ids=\"\${2:id1,id2}\"", "\tapp:flow_wrapMode=\"\${3:aligned}\"\$0 />"))
        ),
        XmlSnippetScope.ATTRIBUTE to listOf(
            DefaultSnippet("id", "android:id", arrayOf("android:id=\"@+id/\${1:id_name}\"")),
            DefaultSnippet("lw", "android:layout_width", arrayOf("android:layout_width=\"\${1:match_parent}\"")),
            DefaultSnippet("lh", "android:layout_height", arrayOf("android:layout_height=\"\${1:match_parent}\"")),
            DefaultSnippet("vis", "android:visibility", arrayOf("android:visibility=\"\${1:visible}\"")),
            DefaultSnippet("grav", "android:gravity", arrayOf("android:gravity=\"\${1:center}\"")),
            DefaultSnippet("lgrav", "android:layout_gravity", arrayOf("android:layout_gravity=\"\${1:center}\"")),
            DefaultSnippet("padding", "android:padding", arrayOf("android:padding=\"\${1:16dp}\"")),
            DefaultSnippet("margin", "android:layout_margin", arrayOf("android:layout_margin=\"\${1:16dp}\"")),
            DefaultSnippet("behavior", "app:layout_behavior", arrayOf("app:layout_behavior=\"\${1:@string/appbar_scrolling_view_behavior}\"")),
            DefaultSnippet("scrollflags", "app:layout_scrollFlags", arrayOf("app:layout_scrollFlags=\"\${1:scroll|exitUntilCollapsed}\"")),
            DefaultSnippet("anchor", "app:layout_anchor", arrayOf("app:layout_anchor=\"@+id/\${1:id_name}\"")),
            DefaultSnippet("anchorgrav", "app:layout_anchorGravity", arrayOf("app:layout_anchorGravity=\"\${1:bottom|end}\"")),
            DefaultSnippet("collapsemode", "app:layout_collapseMode", arrayOf("app:layout_collapseMode=\"\${1:pin}\"")),
            DefaultSnippet("parallax", "app:layout_collapseParallaxMultiplier", arrayOf("app:layout_collapseParallaxMultiplier=\"\${1:0.7}\"")),
            DefaultSnippet("bottomsheet_behavior", "BottomSheetBehavior", arrayOf("app:layout_behavior=\"com.google.android.material.bottomsheet.BottomSheetBehavior\"")),
            DefaultSnippet("hide_behavior", "HideBottomViewOnScrollBehavior", arrayOf("app:layout_behavior=\"com.google.android.material.behavior.HideBottomViewOnScrollBehavior\"")),
            DefaultSnippet("ctt", "app:layout_constraintTop_toTopOf", arrayOf("app:layout_constraintTop_toTopOf=\"\${1:parent}\"")),
            DefaultSnippet("ctb", "app:layout_constraintTop_toBottomOf", arrayOf("app:layout_constraintTop_toBottomOf=\"@+id/\${1:id_name}\"")),
            DefaultSnippet("cbt", "app:layout_constraintBottom_toTopOf", arrayOf("app:layout_constraintBottom_toTopOf=\"@+id/\${1:id_name}\"")),
            DefaultSnippet("cbb", "app:layout_constraintBottom_toBottomOf", arrayOf("app:layout_constraintBottom_toBottomOf=\"\${1:parent}\"")),
            DefaultSnippet("css", "app:layout_constraintStart_toStartOf", arrayOf("app:layout_constraintStart_toStartOf=\"\${1:parent}\"")),
            DefaultSnippet("cee", "app:layout_constraintEnd_toEndOf", arrayOf("app:layout_constraintEnd_toEndOf=\"\${1:parent}\"")),
            DefaultSnippet("chb", "app:layout_constraintHorizontal_bias", arrayOf("app:layout_constraintHorizontal_bias=\"\${1:0.5}\"")),
            DefaultSnippet("cvb", "app:layout_constraintVertical_bias", arrayOf("app:layout_constraintVertical_bias=\"\${1:0.5}\"")),
            DefaultSnippet("ratio", "app:layout_constraintDimensionRatio", arrayOf("app:layout_constraintDimensionRatio=\"\${1:1:1}\"")),
            DefaultSnippet("hchain", "app:layout_constraintHorizontal_chainStyle", arrayOf("app:layout_constraintHorizontal_chainStyle=\"\${1:packed}\"")),
            DefaultSnippet("vchain", "app:layout_constraintVertical_chainStyle", arrayOf("app:layout_constraintVertical_chainStyle=\"\${1:packed}\"")),
            DefaultSnippet("wpercent", "app:layout_constraintWidth_percent", arrayOf("app:layout_constraintWidth_percent=\"\${1:0.5}\"")),
            DefaultSnippet("hpercent", "app:layout_constraintHeight_percent", arrayOf("app:layout_constraintHeight_percent=\"\${1:0.5}\"")),
            DefaultSnippet("ttext", "tools:text", arrayOf("tools:text=\"\${1:Text}\"")),
            DefaultSnippet("tlist", "tools:listitem", arrayOf("tools:listitem=\"@layout/\${1:item_layout}\"")),
            DefaultSnippet("tvis", "tools:visibility", arrayOf("tools:visibility=\"\${1:visible}\"")),
            DefaultSnippet("elevation", "app:cardElevation", arrayOf("app:cardElevation=\"\${1:4dp}\"")),
            DefaultSnippet("scolor", "app:strokeColor", arrayOf("app:strokeColor=\"\${1:@color/colorOutline}\"")),
            DefaultSnippet("swidth", "app:strokeWidth", arrayOf("app:strokeWidth=\"\${1:1dp}\"")),
            DefaultSnippet("lweight", "android:layout_weight", arrayOf("android:layout_weight=\"\${1:1}\"")),
            DefaultSnippet("tint", "android:tint", arrayOf("android:tint=\"\${1:?attr/colorControlNormal}\"")),
            DefaultSnippet("background", "android:background", arrayOf("android:background=\"\${1:?attr/colorSurface}\"")),
            DefaultSnippet("foreground", "android:foreground", arrayOf("android:foreground=\"\${1:?attr/selectableItemBackground}\"")),
            DefaultSnippet("minwidth", "android:minWidth", arrayOf("android:minWidth=\"\${1:48dp}\"")),
            DefaultSnippet("minheight", "android:minHeight", arrayOf("android:minHeight=\"\${1:48dp}\""))
        ),
        XmlSnippetScope.VALUE to listOf(
            //not needed the make duplicate suggestions
//            DefaultSnippet("true", "Boolean true", arrayOf("true")),
//            DefaultSnippet("false", "Boolean false", arrayOf("false")),
//            DefaultSnippet("match", "match_parent", arrayOf("match_parent")),
//            DefaultSnippet("wrap", "wrap_content", arrayOf("wrap_content")),
//            DefaultSnippet("visible", "visible", arrayOf("visible")),
//            DefaultSnippet("gone", "gone", arrayOf("gone")),
//            DefaultSnippet("invisible", "invisible", arrayOf("invisible"))
        )
    )

    fun addSnippets(
        builder: CompletionList.Builder,
        currentScope: XmlSnippetScope,
        partial: String
    ) {
        val lowercasePartial = partial.lowercase()
        val addedPrefixes = mutableSetOf<String>()

        val scopePriority = XmlSnippetScope.entries.associateWith {
            if (it == currentScope) 0 else 1
        }

        for (scope in XmlSnippetScope.entries) {
            val priority = scopePriority[scope] ?: 1
            val scopeSnippets = mutableListOf<ISnippet>()

            // Add manual snippets
            MANUAL_SNIPPETS[scope]?.let { scopeSnippets.addAll(it) }

            // Add repository snippets
            try {
                XmlSnippetRepository.snippets[scope]?.let { scopeSnippets.addAll(it) }
            } catch (ignored: Exception) {}

            for (snippet in scopeSnippets) {
                if (snippet.prefix.lowercase().startsWith(lowercasePartial) && addedPrefixes.add(snippet.prefix)) {
                    val item = createSnippetItem(snippet, partial.length)

                    // Sort by: priority group -> alphabetical prefix
                    item.setSortText("%03d_%s".format(priority, snippet.prefix))
                    builder.addItem(item)
                }
            }
        }
    }

    private fun createSnippetItem(snippet: ISnippet, partialLength: Int): SnippetCompletionItem {
        val body = snippet.body.joinToString("\n")
        val codeSnippet = CodeSnippetParser.parse(body)
        codeSnippet.checkContent()
        val description = SnippetDescription(partialLength, codeSnippet, true)
        return SnippetCompletionItem(snippet.prefix, snippet.description, description)
    }
    private fun createXmlSnippetItem(snippet: ISnippet, partialLength: Int): CompletionItem {
        val body = snippet.body.joinToString("\n")

        return CompletionItem(snippet.prefix).apply {
            desc = snippet.description
            commitText = body
            cursorOffset = body.length
            iconKind = DrawableKind.Snippet
            sortText =  ""
            setInsertHandler(DefaultXmlInsertHandler(this))
        }
    }
}
