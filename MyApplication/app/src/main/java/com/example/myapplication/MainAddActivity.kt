package com.example.myapplication

import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
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

    private lateinit var db: LocalDatabase

    // main
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.main_add, container, false)
    }

    // 뷰 생성 시 실행되는 함수
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = LocalDatabase.getDatabase(requireContext())
        setupViewAndButton(view)
    }

    // View 및 버튼 액션 통합 함수
    private fun setupViewAndButton(view: View) {
        editText = view.findViewById(R.id.EditID)
        selectedDateTextView = view.findViewById(R.id.selectedDateTextView)

        // 날짜 선택 버튼 클릭 리스너
        view.findViewById<Button>(R.id.buttonSelectDate).setOnClickListener {
            showDatePickerDialog()
        }

        // 저장 버튼 클릭 리스너
        view.findViewById<Button>(R.id.buttonSave).setOnClickListener {
            saveData()
        }
    }
    
    // 날짜 선택 시 생성되는 화면
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

    // 저장 버튼 터치 시 작동하는 Db 저장 전 미입력 방지 구문
    private fun saveData() {
        val info = editText.text.toString()
        val selectedDate = selectedDateTextView.text.toString()

        if (info.isNotEmpty() && selectedDate != "선택된 날짜 없음") {
            saveToDatabase(selectedDate, info)
            dismiss() // 다이얼로그 닫기
        } else {
            Toast.makeText(requireContext(), "내용과 날짜를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    // saveData 함수에서 실제 DB에 저장되는 로직
    private fun saveToDatabase(selectedDate: String, info: String) {
        // Room DB에 데이터를 저장하는 함수
        val scheduleInfo = DailySchedule(
            Date = selectedDate,
            Info = info
        )

        // IO 스레드에서 비동기로 데이터베이스 작업을 수행하기 위해 별도의 CoroutineScope 사용
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.getDailyScheduleDao().insertDailyScheduleInfo(scheduleInfo)
                println("insert ok: $selectedDate, : $info")
            } catch (e: Exception) {
                Log.e("MainAddActivity", "Failed to save DailySchedule scheduleInfo: ${e.message}")
            }
        }
    }

}


