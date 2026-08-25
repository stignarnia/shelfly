package xyz.stignarnia.ui_base.common.sheets.sort_order

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.core.view.children
import androidx.fragment.app.setFragmentResult
import xyz.stignarnia.ui_base.BaseBottomSheetFragment
import xyz.stignarnia.ui_base.R
import xyz.stignarnia.ui_base.common.sheets.sort_order.views.SortOrderItemView
import xyz.stignarnia.ui_base.databinding.ViewSortOrderBinding
import xyz.stignarnia.ui_base.utilities.extensions.colorFromAttr
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.requireSerializable
import xyz.stignarnia.ui_base.utilities.extensions.requireString
import xyz.stignarnia.ui_base.utilities.extensions.requireStringArray
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_REQUEST_KEY
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_SELECTED_NEW_AT_TOP
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_SELECTED_SORT_ORDER
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_SELECTED_SORT_TYPE
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_SORT_ORDERS
import xyz.stignarnia.ui_navigation.java.NavigationArgs.REQUEST_SORT_ORDER
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SortOrderBottomSheet : BaseBottomSheetFragment(R.layout.view_sort_order) {

  companion object {
    fun createBundle(
      options: List<SortOrder>,
      selectedOrder: SortOrder,
      selectedType: SortType,
      requestKey: String = REQUEST_SORT_ORDER,
      newAtTop: Pair<Boolean, Boolean> = Pair(false, false),
    ) = Bundle().apply {
      putStringArrayList(ARG_SORT_ORDERS, ArrayList(options.map { it.name }))
      putSerializable(ARG_SELECTED_SORT_ORDER, selectedOrder)
      putSerializable(ARG_SELECTED_SORT_TYPE, selectedType)
      putSerializable(ARG_SELECTED_NEW_AT_TOP, newAtTop)
      putString(ARG_REQUEST_KEY, requestKey)
    }
  }

  private val binding by viewBinding(ViewSortOrderBinding::bind)

  private val requestKey by lazy { requireString(ARG_REQUEST_KEY, default = REQUEST_SORT_ORDER) }
  private val initialSortOrder by lazy { requireSerializable<SortOrder>(ARG_SELECTED_SORT_ORDER) }
  private val initialSortType by lazy { requireSerializable<SortType>(ARG_SELECTED_SORT_TYPE) }
  private val initialNewAtTop by lazy { requireSerializable<Pair<Boolean, Boolean>>(ARG_SELECTED_NEW_AT_TOP) }
  private val initialOptions by lazy { requireStringArray(ARG_SORT_ORDERS).map { SortOrder.valueOf(it) } }

  private lateinit var selectedSortOrder: SortOrder
  private lateinit var selectedSortType: SortType

  override fun getTheme(): Int = R.style.CustomBottomSheetDialog

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    selectedSortOrder = initialSortOrder
    selectedSortType = initialSortType
    setupView()
  }

  private fun setupView() {
    with(binding) {
      viewSortOrderItemsLayout.removeAllViews()
      initialOptions.forEach { item ->
        val itemView = SortOrderItemView(requireContext()).apply {
          onItemClickListener = itemClickListener
          bind(item, initialSortType, item == initialSortOrder)
        }
        viewSortOrderItemsLayout.addView(itemView)
      }

      with(viewSortOrderNewCheckbox) {
        visibleIf(initialNewAtTop.first)
        setOnCheckedChangeListener { _, isChecked ->
          val color = if (isChecked) android.R.attr.textColorPrimary else android.R.attr.textColorSecondary
          val typeface = if (isChecked) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
          setTextColor(context.colorFromAttr(color))
          setTypeface(typeface)
        }
        isChecked = initialNewAtTop.second
      }

      viewSortOrderButtonApply.onClick { onApplySortOrder() }
    }
  }

  private fun onItemClicked(
    sortOrder: SortOrder,
    sortType: SortType,
  ) {
    binding.viewSortOrderItemsLayout.children.forEach { child ->
      with(child as SortOrderItemView) {
        if (sortOrder == child.sortOrder) {
          if (sortOrder == selectedSortOrder) {
            val newSortType = if (sortType == SortType.ASCENDING) SortType.DESCENDING else SortType.ASCENDING
            selectedSortType = newSortType
            bind(sortOrder, newSortType, true, animate = true)
          } else {
            bind(sortOrder, selectedSortType, true)
          }
          selectedSortOrder = sortOrder
        } else {
          bind(child.sortOrder, child.sortType, false)
        }
      }
    }
  }

  private fun onApplySortOrder() {
    val selectedNewAtTop = binding.viewSortOrderNewCheckbox.isChecked

    if (selectedSortOrder != initialSortOrder ||
      initialSortType != selectedSortType ||
      initialNewAtTop.second != selectedNewAtTop
    ) {
      val result = Bundle().apply {
        putSerializable(ARG_SELECTED_SORT_ORDER, selectedSortOrder)
        putSerializable(ARG_SELECTED_SORT_TYPE, selectedSortType)
        putBoolean(ARG_SELECTED_NEW_AT_TOP, selectedNewAtTop)
      }
      setFragmentResult(requestKey, result)
    }

    closeSheet()
  }

  private val itemClickListener: (SortOrder, SortType) -> Unit =
    { sortOrder, sortType -> onItemClicked(sortOrder, sortType) }
}
