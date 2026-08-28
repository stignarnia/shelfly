package xyz.stignarnia.uiBase.common.sheets.dateSelection

import android.os.Bundle
import android.os.Parcelable
import android.text.format.DateFormat
import android.view.View
import androidx.fragment.app.setFragmentResult
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import xyz.stignarnia.common.extensions.dateFromMillis
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.toLocalZone
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.common.extensions.toUtcZone
import xyz.stignarnia.uiBase.BaseBottomSheetFragment
import xyz.stignarnia.uiBase.R
import xyz.stignarnia.uiBase.databinding.ViewDateSelectionBinding
import xyz.stignarnia.uiBase.dates.DateFormatProvider
import xyz.stignarnia.uiBase.utilities.TipsHost
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.optionalSerializable
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.Tip.DATE_SELECTION_DEFAULTS
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_OPTIONS
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@AndroidEntryPoint
class DateSelectionBottomSheet : BaseBottomSheetFragment(R.layout.view_date_selection) {
  companion object {
    const val REQUEST_DATE_SELECTION = "REQUEST_DATE_SELECTION"
    const val RESULT_DATE_SELECTION = "RESULT_DATE_SELECTION"

    fun createBundle(releaseDate: ZonedDateTime?): Bundle =
      Bundle().apply {
        putSerializable(ARG_OPTIONS, releaseDate)
      }
  }

  private val binding by viewBinding(ViewDateSelectionBinding::bind)
  private val releaseDate by lazy { optionalSerializable<ZonedDateTime>(ARG_OPTIONS) }

  override fun getTheme(): Int = R.style.CustomBottomSheetDialog

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupTip()
  }

  private fun setupView() {
    with(binding) {
      cancelButton.onClick { closeSheet() }
      dateNowButton.onClick {
        closeSheet()
        setFragmentResult(
          requestKey = REQUEST_DATE_SELECTION,
          result =
            Bundle().apply {
              putParcelable(RESULT_DATE_SELECTION, Result.Now)
            },
        )
      }
      dateCustomButton.onClick { openDateSelectionDialog() }
      with(dateReleaseButton) {
        if (releaseDate != null) {
          isEnabled = true
          alpha = 1F
          val dateFormat = DateTimeFormatter.ofPattern(DateFormatProvider.DAY_1)
          dateReleaseButtonLabel.text = releaseDate?.toLocalZone()?.format(dateFormat)
          dateReleaseButtonLabel.visible()
        } else {
          isEnabled = false
          alpha = 0.3F
          dateReleaseButtonLabel.text = null
          dateReleaseButtonLabel.gone()
        }
        onClick { onReleaseDateSelected() }
      }
    }
  }

  private fun setupTip() {
    val isShown = (requireActivity() as TipsHost).isTipShown(DATE_SELECTION_DEFAULTS)
    with(binding) {
      defaultsTipText.visibleIf(!isShown)
      defaultsTipOkButton.visibleIf(!isShown)
      if (!isShown) {
        defaultsTipOkButton.onClick {
          (requireActivity() as TipsHost).setTipShow(DATE_SELECTION_DEFAULTS)
          setupTip()
        }
      }
    }
  }

  private fun openDateSelectionDialog() {
    val now = nowUtc().toLocalZone()
    val dialog =
      MaterialDatePicker.Builder
        .datePicker()
        .setCalendarConstraints(
          CalendarConstraints
            .Builder()
            .setFirstDayOfWeek(Calendar.MONDAY)
            .build(),
        ).setTheme(R.style.ShelflyDatePicker)
        .setSelection(now.toMillis() + (now.offset.totalSeconds * 1000))
        .build()
    dialog.addOnPositiveButtonClickListener {
      openTimeSelectionDialog(now, dateFromMillis(it).withZoneSameLocal(now.zone))
    }
    dialog.show(childFragmentManager, "DatePicker")
  }

  private fun openTimeSelectionDialog(
    now: ZonedDateTime,
    selectedDate: ZonedDateTime,
  ) {
    val is24HourFormat = DateFormat.is24HourFormat(requireContext())

    val dialog =
      MaterialTimePicker
        .Builder()
        .setTheme(R.style.ShelflyTimePicker)
        .setTimeFormat(if (is24HourFormat) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
        .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
        .setHour(now.hour)
        .setMinute(now.minute)
        .build()

    dialog.addOnPositiveButtonClickListener {
      onDateTimeSelected(
        selectedDate = selectedDate,
        selectedHour = dialog.hour,
        selectedMinute = dialog.minute,
      )
    }

    dialog.show(childFragmentManager, "TimePicker")
  }

  private fun onDateTimeSelected(
    selectedDate: ZonedDateTime,
    selectedHour: Int,
    selectedMinute: Int,
  ) {
    val resultDate =
      selectedDate
        .withHour(selectedHour)
        .withMinute(selectedMinute)
        .toUtcZone()

    closeSheet()

    val result =
      Bundle().apply {
        putParcelable(RESULT_DATE_SELECTION, Result.CustomDate(resultDate))
      }
    setFragmentResult(REQUEST_DATE_SELECTION, result)
  }

  private fun onReleaseDateSelected() {
    val resultDate =
      releaseDate
        ?.toLocalZone()
        ?.withHour(20)
        ?.withMinute(0)
        ?.toUtcZone()
        ?: nowUtc()

    closeSheet()

    setFragmentResult(
      requestKey = REQUEST_DATE_SELECTION,
      result =
        Bundle().apply {
          putParcelable(RESULT_DATE_SELECTION, Result.ReleaseDate(resultDate))
        },
    )
  }

  sealed interface Result : Parcelable {
    @Parcelize
    data object Now : Result

    @Parcelize
    data class ReleaseDate(
      val date: ZonedDateTime,
    ) : Result

    @Parcelize
    data class CustomDate(
      val date: ZonedDateTime,
    ) : Result
  }
}
