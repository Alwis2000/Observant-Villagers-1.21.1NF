package com.stereowalker.obville.config;

import com.stereowalker.unionlib.config.ConfigObject;
import com.stereowalker.unionlib.config.UnionConfig;
import com.stereowalker.unionlib.util.ScreenHelper.ScreenOffset;

import com.stereowalker.unionlib.config.ConfigSide;

@UnionConfig(folder = "Obville Configs", name = "client", translatableName = "config.obville.client.file", autoReload = true, appendWithType = false)
public class ClientConfig implements ConfigObject {	
	
	@UnionConfig.Entry(name = "Welcome Color", side = ConfigSide.Client)
	public int welcome = 0x4FD65F;
	
	@UnionConfig.Entry(name = "Neutral Color", side = ConfigSide.Client)
	public int neutral = 0xFFFFFF;
	
	@UnionConfig.Entry(name = "Weary Color", side = ConfigSide.Client)
	public int weary = 0x703E24;
	
	@UnionConfig.Entry(name = "Distrusted Color", side = ConfigSide.Client)
	public int distrusted = 0xBE0606;
	
	@UnionConfig.Entry(name = "Exiled Color", side = ConfigSide.Client)
	public int exiled = 0x555555;
	
	@UnionConfig.Entry(name = "Reputation Text Position", side = ConfigSide.Client)
	public ScreenOffset reputationPosition = ScreenOffset.TOP;
	
}
