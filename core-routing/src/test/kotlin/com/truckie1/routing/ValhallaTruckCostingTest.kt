package com.truckie1.routing

import org.junit.Assert.assertTrue
import org.junit.Test

class ValhallaTruckCostingTest {

    @Test
    fun optionsIncludeVehicleDimensionsFromModel() {
        val model = GraphHopperCustomModelStub.default()
        val json = ValhallaTruckCosting.optionsJson(model)
        assertTrue(json.contains("\"height\": 4.0"))
        assertTrue(json.contains("\"width\": 2.5"))
        assertTrue(json.contains("\"length\": 12.0"))
        assertTrue(json.contains("\"weight\": 12.0"))
        assertTrue(json.contains("use_tolls"))
        assertTrue(json.contains("low_class_factor"))
    }
}
