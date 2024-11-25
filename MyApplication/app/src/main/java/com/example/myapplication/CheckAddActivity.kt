package com.example.myapplication

import android.os.Bundle
import android.util.Log
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

// 새로운 체크리스트 항목을 추가하기 위한 BottomSheetDialogFragment
class CheckAddActivity(private val onSave: (ChecklistItem) -> Unit) : BottomSheetDialogFragment() {
    // 뷰 요소 초기화
    private lateinit var titleEditText: EditText
    private lateinit var periodSpinner: Spinner
    private lateinit var weekDaySpinner: Spinner
    private lateinit var monthDaySpinner: Spinner
    private lateinit var saveButton: Button

    // 레이아웃 설정
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottomsheet_checklist_add, container, false)
    }

    // 뷰가 생성되었을 때 실행되는 함수
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)  // 뷰 요소 초기화
        setupPeriodSpinner()    // 주기 선택 스피너 설정
        setupSaveButton()       // 저장 버튼 리스너 설정
    }

    // 뷰 요소와 XML 레이아웃을 연결하는 함수
    private fun initializeViews(view: View) {
        titleEditText = view.findViewById(R.id.edittext_checklist_input)             // 제목 입력란
        periodSpinner = view.findViewById(R.id.spinner_checklist_periodDefault)       // 주기 스피너
        weekDaySpinner = view.findViewById(R.id.spinner_checklist_periodWeek)         // 요일 스피너
        monthDaySpinner = view.findViewById(R.id.spinner_checklist_periodMonth)       // 날짜 스피너
        saveButton = view.findViewById(R.id.button_checklist_saveData)                // 저장 버튼
    }

    // 주기 선택 스피너 설정 함수
    private fun setupPeriodSpinner() {
        // 주기 선택 옵션 설정
        val periods = arrayOf("선택하세요", "매일", "매주", "매월")
        val periodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periods)
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodSpinner.adapter = periodAdapter

        // 주기 스피너 선택 이벤트 처리
        periodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (periods[position]) {
                    "매주" -> {
                        // '매주' 선택 시 요일 스피너만 보이도록 설정
                        weekDaySpinner.visibility = View.VISIBLE
                        monthDaySpinner.visibility = View.GONE
                        setupWeekDaySpinner()  // 요일 스피너 초기화
                    }
                    "매월" -> {
                        // '매월' 선택 시 날짜 스피너만 보이도록 설정
                        weekDaySpinner.visibility = View.GONE
                        monthDaySpinner.visibility = View.VISIBLE
                        setupMonthDaySpinner() // 날짜 스피너 초기화
                    }
                    else -> {
                        // 다른 옵션 선택 시 요일과 날짜 스피너 숨김
                        weekDaySpinner.visibility = View.GONE
                        monthDaySpinner.visibility = View.GONE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // 요일 선택 스피너 설정 함수
    private fun setupWeekDaySpinner() {
        val weekDays = arrayOf("일", "월", "화", "수", "목", "금", "토") // 요일 이름 배열
        val weekDayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, weekDays)
        weekDayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        weekDaySpinner.adapter = weekDayAdapter
    }

    // 날짜 선택 스피너 설정 함수
    private fun setupMonthDaySpinner() {
        val monthDays = (1..31).map { it.toString() }.toTypedArray() // 1일부터 31일까지 날짜 설정
        val monthDayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, monthDays)
        monthDayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthDaySpinner.adapter = monthDayAdapter
    }

    // 저장 버튼 클릭 이벤트 설정 함수
    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            val title = titleEditText.text.toString().trim() // 제목 입력값
            val period = periodSpinner.selectedItem.toString().trim() // 선택된 주기
            val weekDay = if (weekDaySpinner.visibility == View.VISIBLE) {
                weekDaySpinner.selectedItem.toString() // 선택된 요일 이름으로 저장 (예: "월", "화")
            } else {
                null
            }
            val monthDay = if (monthDaySpinner.visibility == View.VISIBLE) {
                monthDaySpinner.selectedItem.toString().trim()
            } else {
                null
            }

            // 입력값 유효성 검사
            if (title.isEmpty() || period == "선택하세요") {
                Toast.makeText(requireContext(), "제목과 주기를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                // 디버깅용 로그 추가
                Log.d("CheckAddActivity", "Saving ChecklistItem - Title: $title, Period: $period, WeekDay: $weekDay, MonthDay: $monthDay")

                // 새 ChecklistItem 생성하여 onSave 콜백 호출
                val newItem = ChecklistItem(
                    cTitle = title,
                    period = period,
                    weekDay = weekDay, // 요일 이름 저장
                    monthDay = monthDay
                )
                onSave(newItem) // 저장 콜백 호출
                dismiss() // 다이얼로그 닫기
            }
        }
    }
}
