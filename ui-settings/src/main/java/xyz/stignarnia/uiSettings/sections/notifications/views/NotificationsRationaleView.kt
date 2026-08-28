package xyz.stignarnia.uiSettings.sections.notifications.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import xyz.stignarnia.uiSettings.databinding.ViewNotificationsRationaleBinding

class NotificationsRationaleView : FrameLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewNotificationsRationaleBinding.inflate(LayoutInflater.from(context), this)
}
