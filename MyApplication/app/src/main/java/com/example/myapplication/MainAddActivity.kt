package com.example.myapplication

import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainAddActivity : BottomSheetDialogFragment() {
    private lateinit var editText: EditText
    private lateinit var selectedDateTextView: TextView
    private lateinit var buttonSelectDate: Button
    private lateinit var buttonSave: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.main_add, container, false) // layout 파일 이름에 맞게 수정

        // 뷰 초기화
        editText = view.findViewById(R.id.EditID)
        selectedDateTextView = view.findViewById(R.id.selectedDateTextView)
        buttonSelectDate = view.findViewById(R.id.buttonSelectDate)
        buttonSave = view.findViewById(R.id.buttonSave)

        // 날짜 선택 버튼 클릭 리스너
        buttonSelectDate.setOnClickListener {
            showDatePickerDialog()
        }

        // 저장 버튼 클릭 리스너
        buttonSave.setOnClickListener {
            saveData()
        }

        return view // 뷰를 반환
    }

    private fun showDatePickerDialog() {
        // 현재 날짜 가져오기
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // 날짜 선택 다이얼로그 생성
        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            // 선택한 날짜를 TextView에 표시
            val selectedDate = "$selectedYear-${selectedMonth + 1}-$selectedDay"
            selectedDateTextView.text = selectedDate
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun saveData() {
        val content = editText.text.toString()
        val selectedDate = selectedDateTextView.text.toString()

        if (content.isNotEmpty() && selectedDate != "선택된 날짜 없음") {
            // Room DB에 데이터 저장

            dismiss() // 다이얼로그 닫기
        } else {
            Toast.makeText(requireContext(), "내용과 날짜를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
        }
    }
}
