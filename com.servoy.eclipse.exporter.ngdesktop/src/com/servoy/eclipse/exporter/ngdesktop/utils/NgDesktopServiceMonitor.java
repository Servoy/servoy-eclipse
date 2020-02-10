package com.servoy.eclipse.exporter.ngdesktop.utils;

import org.eclipse.core.runtime.IProgressMonitor;

/*
 * NgDesktop service is generating build files having big variation in data size written to the disk / time unit.
 * This class is used to simulate a smooth progress - keeping in the same time a pretty accurate progress of the real build  
 */
public class NgDesktopServiceMonitor implements Runnable, IProgressMonitor
{
	private int REFRESH_INTERVAL = 500; // ms, 500 = 2 refreshes / seconds
	private int SLOW_REFRESH_INTERVAL = 1000;
	private IProgressMonitor monitor;
	private int totalSteps;
	private int currentStep;
	private int targetStep;
	private int progressUnit;
	Thread threadMonitor;
	private boolean stopServiceMonitor = false;

	public NgDesktopServiceMonitor(IProgressMonitor monitor)
	{
		this.monitor = monitor;
	}

	public void startChase(String name, int totalWork, int duration)
	{
		totalSteps = totalWork;
		currentStep = 0;
		progressUnit = Math.round(((float)totalWork / duration) / (1000 / REFRESH_INTERVAL));
		monitor.beginTask(name, totalWork);
		threadMonitor = new Thread(this);
		threadMonitor.start();
	}

	public void endChase()
	{
		stopServiceMonitor = true;
		if (monitor.isCanceled())
			return;
		fillRemainingSteps();
		waitForRefresh(REFRESH_INTERVAL);// provide time for visual feedback

	}

	public void fillRemainingSteps()
	{
		monitor.worked(totalSteps - currentStep);
	}

	@Override
	public void run()
	{
		while (!stopServiceMonitor)
		{
			if (currentStep < targetStep)
			{
				currentStep += progressUnit;
				monitor.worked(progressUnit);
				waitForRefresh(REFRESH_INTERVAL);
			}
			else
			{
				currentStep += 1;
				monitor.worked(1);
				waitForRefresh(SLOW_REFRESH_INTERVAL);
			}
		}
	}

	private void waitForRefresh(int refreshValue)
	{
		try
		{
			Thread.sleep(refreshValue);
		}
		catch (InterruptedException e)
		{
		}
	}

	public void setTargetStep(int step)
	{
		this.targetStep = step;
	}

	public void beginTask(String taskName, int totalWork)
	{
		monitor.beginTask(taskName, totalWork);
	}

	public void setTaskName(String taskName)
	{
		monitor.setTaskName(taskName);
	}

	public void worked(int work)
	{
		monitor.worked(work);
	}

	public boolean isCanceled()
	{
		return monitor.isCanceled();
	}

	public void done()
	{
		monitor.done();
	}

	@Override
	public void internalWorked(double work)
	{
		monitor.internalWorked(work);
	}

	@Override
	public void setCanceled(boolean value)
	{
		monitor.setCanceled(value);
	}

	@Override
	public void subTask(String name)
	{
		monitor.subTask(name);
	}
}