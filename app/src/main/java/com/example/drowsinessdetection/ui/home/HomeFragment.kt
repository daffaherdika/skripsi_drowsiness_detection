package com.example.drowsinessdetection.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.drowsinessdetection.R
import com.example.drowsinessdetection.databinding.FragmentHomeBinding
import com.example.drowsinessdetection.ui.detection.DetectionFragment

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStartDetection.setOnClickListener {
            val useClahe = binding.switchClahe.isChecked
            val fragment = DetectionFragment()
            fragment.arguments = Bundle().apply {
                putBoolean("useClahe", useClahe)
            }
            parentFragmentManager.commit {
                replace(R.id.nav_host_fragment, fragment)
                addToBackStack(null)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}