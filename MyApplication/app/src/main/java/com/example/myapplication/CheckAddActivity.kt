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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CheckAddActivity : BottomSheetDialogFragment() {

    private lateinit var titleEditText: EditText
    private lateinit var periodSpinner: Spinner
    private lateinit var weekDaySpinner: Spinner
    private lateinit var monthDaySpinner: Spinner
    private lateinit var saveButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.checklist_add, container, false) // 변경: layout 파일 이름에 맞게 수정
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleEditText = view.findViewById(R.id.CheckPageEditID)
        periodSpinner = view.findViewById(R.id.CheckPageperiodSpinner)
        weekDaySpinner = view.findViewById(R.id.CheckPageweekDaySpinner)
        monthDaySpinner = view.findViewById(R.id.CheckPagemonthDaySpinner)
        saveButton = view.findViewById(R.id.CheckPagebuttonSave)

        // 주기 선택 Spinner 설정
        val periods = arrayOf("선택하세요", "매일", "매주", "매월")
        val periodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periods)
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodSpinner.adapter = periodAdapter

        // 주기 선택 리스너
        // 주기 선택 리스너
        periodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (periods[position]) {
                    "매주" -> {
                        weekDaySpinner.visibility = View.VISIBLE
                        monthDaySpinner.visibility = View.GONE
                        setupWeekDaySpinner() // 요일 Spinner 설정
                    }
                    "매월" -> {
                        weekDaySpinner.visibility = View.GONE
                        monthDaySpinner.visibility = View.VISIBLE
                        setupMonthDaySpinner() // 날짜 Spinner 설정
                    }
                    else -> {
                        weekDaySpinner.visibility = View.GONE
                        monthDaySpinner.visibility = View.GONE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 아무것도 선택되지 않았을 때의 처리
            }
        }

        // 저장 버튼 클릭 리스너
        saveButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val period = periodSpinner.selectedItem.toString()
            val weekDay = if (weekDaySpinner.visibility == View.VISIBLE) weekDaySpinner.selectedItem.toString() else ""
            val monthDay = if (monthDaySpinner.visibility == View.VISIBLE) monthDaySpinner.selectedItem.toString() else ""

            // 데이터 처리 로직 (예: 데이터베이스에 저장 등)
            // 예시: Log 출력
            println("제목: $title, 주기: $period, 요일: $weekDay, 날짜: $monthDay")

            dismiss() // Bottom Sheet 닫기
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

}
