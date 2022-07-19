package com.iosapp.ioslauncher.base

import androidx.databinding.Observable
import androidx.lifecycle.ViewModel

abstract class BaseViewModel : ViewModel() {

    private val observablesAndObservers = mutableListOf<Pair<Observable, Observable.OnPropertyChangedCallback>>()

    fun observe(observable: Observable, observer: (Observable, Int) -> Unit) =
        observablesAndObservers.add(observable to observable.addOnPropertyChangedCallback(observer))

    private fun Observable.addOnPropertyChangedCallback(
        callback: (Observable, Int) -> Unit
    ): Observable.OnPropertyChangedCallback =
        object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) =
                callback(sender, propertyId)
        }.also { addOnPropertyChangedCallback(it) }

    override fun onCleared() {
        observablesAndObservers.forEach { (observable, observer) ->
            observable.removeOnPropertyChangedCallback(observer)
        }
        observablesAndObservers.clear()
        super.onCleared()
    }

}