package com.mobilesorcery.sdk.capabilities.core;

import java.text.MessageFormat;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;

public class AddDeviceFilterChangeRequest extends AbstractChangeRequest {

	private IDeviceFilter filter;
	private String message = "Remove {0} profiles";

	public AddDeviceFilterChangeRequest(MoSyncProject project, IDeviceFilter filter) {
		super(project);
		this.filter = filter;
	}
	
	/**
	 * Sets a message that will be returned by <code>toString</code>.
	 * Optionally, a <code>{0}</code> parameter may be used to 
	 * insert the number of filtered out profiles.
	 * @param message
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	
	public void apply() {
		getProject().getDeviceFilter().addFilter(filter);
	}
	
	public String toString() {
		int filterCount = MoSyncTool.getDefault().getProfiles(filter).length;
		int totalProfileCount = MoSyncTool.getDefault().getProfiles().length;
		int removedCount = totalProfileCount - filterCount;
		return MessageFormat.format(message, removedCount);
	}

}
