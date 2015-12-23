/**
 *    Copyright 2015 Austin Keener & Michael Ritter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.dv8tion.jda.entities.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;

public class VoiceChannelImpl implements VoiceChannel
{
    private final String id;
    private final Guild guild;
    private String name;
    private int position;
    private List<User> connectedUsers = new ArrayList<>();
    private Map<User, PermissionOverride> userPermissionOverrides = new HashMap<>();
    private Map<Role, PermissionOverride> rolePermissionOverrides = new HashMap<>();

    public VoiceChannelImpl(String id, Guild guild)
    {
        this.id = id;
        this.guild = guild;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Guild getGuild()
    {
        return guild;
    }

    @Override
    public int getPosition()
    {
        return position;
    }

    @Override
    public List<User> getUsers()
    {
        return Collections.unmodifiableList(connectedUsers);
    }

    @Override
    public boolean checkPermission(User user, Permission perm)
    {
        //Do we have all permissions possible? (Owner or user has MANAGE_ROLES permission)
        //If we have all permissions possible, then we will be able to see this room.
        if (getGuild().getOwnerId().equals(user.getId())
                || getGuild().getPublicRole().hasPermission(Permission.MANAGE_ROLES)
                || getGuild().getRolesForUser(user).stream().anyMatch(role -> role.hasPermission(Permission.MANAGE_ROLES)))
        {
            return true;
        }

        //Default global permission of @everyone in this guild
        int permission = ((RoleImpl) getGuild().getPublicRole()).getPermissions();
        //override with channel-specific overrides of @everyone
        PermissionOverride override = rolePermissionOverrides.get(getGuild().getPublicRole());
        if (override != null)
        {
            permission = rolePermissionOverrides.get(getGuild().getPublicRole()).apply(permission);
        }

        //handle role-overrides of this user in this channel
        List<Role> rolesOfUser = getGuild().getRolesForUser(user);
        override = null;
        for (Role role : rolesOfUser)
        {
            PermissionOverride po = rolePermissionOverrides.get(role);
            override = (po == null) ? override : ((override == null) ? po : po.after(override));
        }
        if (override != null)
        {
            permission = override.apply(permission);
        }

        //handle user-specific overrides
        PermissionOverride useroverride = userPermissionOverrides.get(user);
        if (useroverride != null)
        {
            permission = useroverride.apply(permission);
        }
        return (permission & (1 << perm.getOffset())) > 0;
    }

    public VoiceChannelImpl setName(String name)
    {
        this.name = name;
        return this;
    }

    public VoiceChannelImpl setPosition(int position)
    {
        this.position = position;
        return this;
    }

    public VoiceChannelImpl setUsers(List<User> connectedUsers)
    {
        this.connectedUsers = connectedUsers;
        return this;
    }

    public List<User> getUsersModifiable()
    {
        return connectedUsers;
    }

    public Map<User, PermissionOverride> getUserPermissionOverrides()
    {
        return userPermissionOverrides;
    }

    public Map<Role, PermissionOverride> getRolePermissionOverrides()
    {
        return rolePermissionOverrides;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof VoiceChannel))
            return false;
        VoiceChannel oVChannel = (VoiceChannel) o;
        return this == oVChannel || this.getId().equals(oVChannel.getId());
    }

    @Override
    public int hashCode()
    {
        return getId().hashCode();
    }
}