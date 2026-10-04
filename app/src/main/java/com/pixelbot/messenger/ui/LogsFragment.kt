package com.pixelbot.messenger.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.pixelbot.messenger.adapter.BotLogAdapter
import com.pixelbot.messenger.databinding.FragmentLogsBinding
import com.pixelbot.messenger.engine.BotController

class LogsFragment : Fragment() {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: BotLogAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BotLogAdapter(BotController.logList)
        binding.rvLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLogs.adapter = adapter

        BotController.logUpdateListener = {
            activity?.runOnUiThread {
                adapter.notifyDataSetChanged()
                if (BotController.logList.isNotEmpty()) {
                    binding.rvLogs.scrollToPosition(0)
                }
            }
        }

        binding.btnClearLogs.setOnClickListener {
            BotController.logList.clear()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BotController.logUpdateListener = null
        _binding = null
    }
}
