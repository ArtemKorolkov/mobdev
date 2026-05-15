package com.student.mobile_dev_laboratory_work_3.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil.load
import com.student.mobile_dev_laboratory_work_3.FaeryTeaApp
import com.student.mobile_dev_laboratory_work_3.databinding.FragmentImageBinding
import com.student.mobile_dev_laboratory_work_3.util.ImageUrlBuilder

/** Полноэкранный просмотр картинки из /img/ */
class ImageFragment : Fragment() {

    private var _binding: FragmentImageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by activityViewModels {
        (requireActivity() as MainActivity).chatViewModelFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.imagePath.observe(viewLifecycleOwner) { path ->
            if (!path.isNullOrBlank()) {
                binding.imageFull.load(ImageUrlBuilder.fullUrl(path)) {
                    crossfade(true)
                }
            }
        }

        binding.buttonCloseImage.setOnClickListener {
            viewModel.closeImage()
            (activity as? MainActivity)?.showPortraitScreen(
                com.student.mobile_dev_laboratory_work_3.data.model.PortraitScreen.MESSAGES,
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): ImageFragment = ImageFragment()
    }
}
