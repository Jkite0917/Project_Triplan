package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.room.Room
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// DateInfoBottomSheet.kt
class MainDateInfoActivity(private val selectedDate: String) : BottomSheetDialogFragment() {

    private lateinit var editText: EditText
    private lateinit var TextView : TextView
    private lateinit var deleteButton: Button
    private lateinit var db: LocalDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.main_calender_datelist, container, false)
        editText = view.findViewById(R.id.MainDateInfoEditText)
        TextView = view.findViewById(R.id.MainDateInfoDateText)
        deleteButton = view.findViewById(R.id.MainDateInfoDeleteButton)

        // Room 데이터베이스 초기화
        db = Room.databaseBuilder(
            requireContext(),
            LocalDatabase::class.java,
            "calendar_database"
        ).build()

        // 데이터 로딩
        loadData()

        // 삭제 버튼 클릭 리스너 설정
        deleteButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                db.getDailyScheduleDao().deleteDailyScheduleInfo(selectedDate)

                // UI 업데이트는 메인 스레드에서 수행
                withContext(Dispatchers.Main) {
                    editText.text = null // EditText 비우기
                    Toast.makeText(requireContext(), "일정이 삭제되었습니다 새로 추가해주세요!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    private fun loadData() {
        TextView.text = selectedDate

        CoroutineScope(Dispatchers.IO).launch {
            val schedule = db.getDailyScheduleDao().getDailyScheduleInfo(selectedDate)
            withContext(Dispatchers.Main) {
                if (schedule != null) {
                    editText.text = Editable.Factory.getInstance().newEditable(schedule)
                } else {
                    editText.setText("")
                    Toast.makeText(context, "해당 날짜에 저장된 일정이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
