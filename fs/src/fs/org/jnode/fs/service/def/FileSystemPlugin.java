/*
 * $Id$
 *
 * JNode.org
 * Copyright (C) 2005 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License 
 * along with this library; if not, write to the Free Software Foundation, 
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */
 
package org.jnode.fs.service.def;

import java.io.VMFileSystemAPI;
import java.io.VMIOUtils;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;

import javax.naming.NamingException;

import org.jnode.driver.Device;
import org.jnode.fs.FileSystem;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.FileSystemType;
import org.jnode.fs.service.FileSystemService;
import org.jnode.naming.InitialNaming;
import org.jnode.plugin.Plugin;
import org.jnode.plugin.PluginDescriptor;
import org.jnode.plugin.PluginException;

/**
 * @author epr
 */
public class FileSystemPlugin extends Plugin implements FileSystemService {

    /** Manager of fs types */
    private final FileSystemTypeManager fsTypeManager;

    /** Manager of mounted filesystems */
    private final FileSystemManager fsm;

    /** The FS-API implementation */
    private final VMFileSystemAPI api;

    /** The mounter */
    private FileSystemMounter mounter;

    /**
     * Create a new instance
     *  
     */
    public FileSystemPlugin(PluginDescriptor descriptor) {
        super(descriptor);
        this.fsTypeManager = new FileSystemTypeManager(descriptor
                .getExtensionPoint("types"));
        this.fsm = new FileSystemManager();
        this.api = new FileSystemAPIImpl(fsm);
    }

    /**
     * Gets all registered file system types. All instances of the returned
     * collection are instanceof FileSystemType.
     */
    public Collection<FileSystemType> fileSystemTypes() {
        return fsTypeManager.fileSystemTypes();
    }

    /**
     * Register a mounted filesystem
     * 
     * @param fs
     */
    public void registerFileSystem(FileSystem fs) {
        fsm.registerFileSystem(fs);
    }

    /**
     * Unregister a mounted filesystem
     * 
     * @param device
     */
    public FileSystem unregisterFileSystem(final Device device) {
        return (FileSystem) AccessController
                .doPrivileged(new PrivilegedAction() {

                    public Object run() {
                        api.rootRemoved(new java.io.File(fsm
                                .getMountPoint(device)));
                        return fsm.unregisterFileSystem(device);
                    }
                });
    }

    /**
     * Gets the filesystem registered on the given device.
     * 
     * @param device
     * @return null if no filesystem was found.
     */
    public FileSystem getFileSystem(Device device) {
        return fsm.getFileSystem(device);
    }

    /**
     * Gets all registered filesystems. All instances of the returned collection
     * are instanceof FileSystem.
     */
    public Collection<FileSystem> fileSystems() {
        return fsm.fileSystems();
    }

    /**
     * Gets the filesystem API.
     */
    public VMFileSystemAPI getApi() {
        return api;
    }

    /**
     * Start this plugin
     */
    protected void startPlugin() throws PluginException {
        try {
            VMIOUtils.setAPI(getApi(), this);
            mounter = new FileSystemMounter();
            InitialNaming.bind(NAME, this);
            mounter.start();
        } catch (NamingException ex) {
            throw new PluginException(ex);
        }
    }

    /**
     * Stop this plugin
     */
    protected void stopPlugin() {
        mounter.stop();
        InitialNaming.unbind(NAME);
        VMIOUtils.resetAPI(this);
        mounter = null;
    }

    /**
     * @see org.jnode.fs.service.FileSystemService#getFileSystemTypeForNameSystemTypes(java.lang.String)
     */
    public FileSystemType getFileSystemTypeForNameSystemTypes(String name)
            throws FileSystemException {
        FileSystemType result = fsTypeManager.getSystemType(name);
        if (result == null) { throw new FileSystemException(
                "Not existent FisleSystemType"); }
        return result;
    }
}
