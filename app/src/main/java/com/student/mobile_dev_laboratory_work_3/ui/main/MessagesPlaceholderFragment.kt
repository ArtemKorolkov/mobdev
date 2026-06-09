package com.student.mobile_dev_laboratory_work_3.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.student.mobile_dev_laboratory_work_3.databinding.FragmentMessagesPlaceholderBinding

/** Заглушка «Выберите чат» в ландшафтной ориентации */
class MessagesPlaceholderFragment : Fragment() {

    private var _binding: FragmentMessagesPlaceholderBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMessagesPlaceholderBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): MessagesPlaceholderFragment = MessagesPlaceholderFragment()
    }
}
