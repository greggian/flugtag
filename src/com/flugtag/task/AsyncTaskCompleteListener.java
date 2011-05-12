package com.flugtag.task;

public interface AsyncTaskCompleteListener<T> {
	public void onTaskComplete(T result);
}
