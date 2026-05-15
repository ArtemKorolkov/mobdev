package com.student.mobile_dev_laboratory_work_3.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.student.mobile_dev_laboratory_work_3.FaeryTeaApp
import com.student.mobile_dev_laboratory_work_3.data.model.PortraitScreen
import com.student.mobile_dev_laboratory_work_3.databinding.FragmentMessagesBinding
import com.student.mobile_dev_laboratory_work_3.util.ErrorMapper

/** Экран сообщений выбранного чата */
class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by activityViewModels {
        (requireActivity() as MainActivity).chatViewModelFactory
    }

    private lateinit var adapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isLandscape = resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE

        binding.buttonBackToChats.visibility =
            if (isLandscape) View.GONE else View.VISIBLE

        adapter = MessageAdapter(
            currentUser = FaeryTeaApp.from(requireActivity().application).sessionManager.username.orEmpty(),
            onImageClick = { path ->
                viewModel.openImage(path)
                (activity as? MainActivity)?.onImageOpened()
            },
        )
        val layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.recyclerMessages.layoutManager = layoutManager
        binding.recyclerMessages.adapter = adapter

        binding.recyclerMessages.addOnScrollListener(
            object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                    if (layoutManager.findFirstVisibleItemPosition() == 0) {
                        viewModel.loadOlderMessages()
                    }
                }
            },
        )

        binding.buttonBackToChats.setOnClickListener {
            viewModel.showChatsScreen()
            (activity as? MainActivity)?.showPortraitScreen(PortraitScreen.CHATS)
        }

        binding.buttonSend.setOnClickListener {
            val text = binding.editMessage.text?.toString().orEmpty()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.editMessage.text?.clear()
            }
        }

        viewModel.selectedChannel?.let { binding.textChatTitle.text = it }

        viewModel.messagesState.observe(viewLifecycleOwner) { state ->
            binding.progressMessages.visibility =
                if (state.isLoading) View.VISIBLE else View.GONE
            binding.buttonSend.isEnabled = !state.sendInProgress
            adapter.submitList(state.messages)

            if (state.messages.isNotEmpty()) {
                binding.recyclerMessages.scrollToPosition(state.messages.size - 1)
            }

            state.errorMessage?.let { code ->
                viewModel.clearMessagesError()
                AlertDialog.Builder(requireContext())
                    .setMessage(ErrorMapper.toUserMessage(requireContext(), code))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }

        viewModel.portraitScreen.observe(viewLifecycleOwner) { /* обновление навигации в Activity */ }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): MessagesFragment = MessagesFragment()
    }
}
