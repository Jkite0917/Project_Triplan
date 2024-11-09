package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CheckAddActivity(private val onSave: (ChecklistItem) -> Unit) : BottomSheetDialogFragment() {
    private lateinit var titleEditText: EditText
    private lateinit var periodSpinner: Spinner
    private lateinit var weekDaySpinner: Spinner
    private lateinit var monthDaySpinner: Spinner
    private lateinit var saveButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottomsheet_checklist_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupPeriodSpinner()
        setupSaveButton()
    }

    private fun initializeViews(view: View) {
        titleEditText = view.findViewById(R.id.edittext_checklist_input)
        periodSpinner = view.findViewById(R.id.spinner_checklist_periodDefault)
        weekDaySpinner = view.findViewById(R.id.spinner_checklist_periodWeek)
        monthDaySpinner = view.findViewById(R.id.spinner_checklist_periodMonth)
        saveButton = view.findViewById(R.id.button_checklist_saveData)
    }

    private fun setupPeriodSpinner() {
        val periods = arrayOf("선택하세요", "매일", "매주", "매월")
        val periodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periods)
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodSpinner.adapter = periodAdapter

        periodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (periods[position]) {
                    "매주" -> {
                        weekDaySpinner.visibility = View.VISIBLE
                        monthDaySpinner.visibility = View.GONE
                        setupWeekDaySpinner()
                    }
                    "매월" -> {
                        weekDaySpinner.visibility = View.GONE
                        monthDaySpinner.visibility = View.VISIBLE
                        setupMonthDaySpinner()
                    }
                    else -> {
                        weekDaySpinner.visibility = View.GONE
                        monthDaySpinner.visibility = View.GONE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupWeekDaySpinner() {
        val weekDays = arrayOf("일", "월", "화", "수", "목", "금", "토")
        val weekDayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, weekDays)
        weekDayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        weekDaySpinner.adapter = weekDayAdapter
    }

    private fun setupMonthDaySpinner() {
        val monthDays = (1..31).map { it.toString() }.toTypedArray()
        val monthDayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, monthDays)
        monthDayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthDaySpinner.adapter = monthDayAdapter
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val period = periodSpinner.selectedItem.toString()
            val weekDay = if (weekDaySpinner.visibility == View.VISIBLE) weekDaySpinner.selectedItem.toString() else null
            val monthDay = if (monthDaySpinner.visibility == View.VISIBLE) monthDaySpinner.selectedItem.toString() else null

            if (title.isEmpty() || period == "선택하세요") {
                Toast.makeText(requireContext(), "제목과 주기를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                val newItem = ChecklistItem(
                    cTitle = title,
                    period = period,
                    weekDay = weekDay,
                    monthDay = monthDay
                )
                onSave(newItem)
                dismiss()
            }
        }
    }
}
