package com.truckie1.app.auto

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template

class TruckieCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = TruckieCarHomeScreen(carContext)
}

class TruckieCarHomeScreen(
    carContext: androidx.car.app.CarContext,
) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        // Cars App 1.7: MessageTemplate.setTitle / setHeaderAction are deprecated in favor of Header.
        val header = Header.Builder()
            .setTitle("Truckie")
            .setStartHeaderAction(Action.APP_ICON)
            .build()
        return MessageTemplate.Builder("Truckie navigation (scaffold)")
            .setHeader(header)
            .build()
    }
}
