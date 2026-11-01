package com.letter.zeropwa

import com.letter.zeropwa.data.PwaSortOrder
import com.letter.zeropwa.data.model.PwaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PwaSortOrderTest {

    @Test
    fun defaultSortOrderIsManual() {
        assertEquals(PwaSortOrder.MANUAL, PwaSortOrder.valueOf("MANUAL"))
        assertTrue("默认自定义排序应按 sort_order 升序", PwaSortOrder.MANUAL.sqlOrderBy.contains("sort_order ASC"))
    }

    @Test
    fun manualReorderListAlgorithm() {
        val item1 = PwaItem(id = 1L, name = "A", url = "https://a.com", startUrl = "https://a.com", sortOrder = 1)
        val item2 = PwaItem(id = 2L, name = "B", url = "https://b.com", startUrl = "https://b.com", sortOrder = 2)
        val item3 = PwaItem(id = 3L, name = "C", url = "https://c.com", startUrl = "https://c.com", sortOrder = 3)

        val list = mutableListOf(item1, item2, item3)

        // 模拟 item3 置顶
        val idx = list.indexOfFirst { it.id == 3L }
        val removed = list.removeAt(idx)
        list.add(0, removed)

        assertEquals(3L, list[0].id)
        assertEquals(1L, list[1].id)
        assertEquals(2L, list[2].id)

        // 模拟当前第二项 (item1) 下移一位
        val downIdx = 1
        val itemToMove = list.removeAt(downIdx)
        list.add(downIdx + 1, itemToMove)

        assertEquals(3L, list[0].id)
        assertEquals(2L, list[1].id)
        assertEquals(1L, list[2].id)
    }
}
