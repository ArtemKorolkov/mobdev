package com.student.mobile_dev_laboratory_work_3.ui.main

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.student.mobile_dev_laboratory_work_3.FaeryTeaApp
import com.student.mobile_dev_laboratory_work_3.databinding.FragmentChatsBinding
import com.student.mobile_dev_laboratory_work_3.ui.login.LoginActivity
import com.student.mobile_dev_laboratory_work_3.util.ErrorMapper
import kotlinx.coroutines.launch

/** Список каналов (чатов) и кнопка выхода */
class ChatsFragment : Fragment() {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by activityViewModels {
        (requireActivity() as MainActivity).chatViewModelFactory
    }

    private lateinit var adapter: ChannelAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ChannelAdapter(
            selectedChannel = viewModel.selectedChannel,
            onChannelClick = { channel ->
                (activity as? MainActivity)?.onChannelSelected(channel)
            },
        )
        binding.recyclerChannels.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerChannels.adapter = adapter

        binding.buttonLogout.setOnClickListener {
            lifecycleScope.launch {
                val session = FaeryTeaApp.from(requireActivity().application).sessionManager
                FaeryTeaApp.from(requireActivity().application).repository.logout()
                session.onLogout()
                startActivity(
                    android.content.Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    },
                )
                requireActivity().finish()
            }
        }

        viewModel.channelsState.observe(viewLifecycleOwner) { state ->
            binding.progressChannels.visibility =
                if (state.isLoading) View.VISIBLE else View.GONE
            adapter.submitList(state.channels, viewModel.selectedChannel)

            state.errorMessage?.let { code ->
                viewModel.clearChannelsError()
                AlertDialog.Builder(requireContext())
                    .setMessage(ErrorMapper.toUserMessage(requireContext(), code))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }

        viewModel.loadChannelsIfNeeded()

        // Обновляем выделение при смене канала (ландшафт)
        viewModel.messagesState.observe(viewLifecycleOwner) {
            adapter.submitList(viewModel.channelsState.value?.channels.orEmpty(), viewModel.selectedChannel)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adapter.submitList(
            viewModel.channelsState.value?.channels.orEmpty(),
            viewModel.selectedChannel,
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): ChatsFragment = ChatsFragment()
    }
}
