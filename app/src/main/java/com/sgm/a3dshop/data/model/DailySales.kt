package com.sgm.a3dshop.data.model

import com.sgm.a3dshop.data.entity.SaleRecord
import java.util.Date

data class DailySales(
    val date: Date,
    val records: List<SaleRecord>,
    val totalAmount: Double
) 