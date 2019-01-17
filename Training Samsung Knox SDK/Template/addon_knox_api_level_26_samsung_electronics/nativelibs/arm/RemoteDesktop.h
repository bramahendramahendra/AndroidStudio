/**
 * Copyright (C) 2012 Samsung Electronics Co., Ltd. All rights reserved.
 *
 * Mobile Communication Division,
 * Digital Media & Communications Business, Samsung Electronics Co., Ltd.
 *
 * This software and its documentation are confidential and proprietary
 * information of Samsung Electronics Co., Ltd.  No part of the software and
 * documents may be copied, reproduced, transmitted, translated, or reduced to
 * any electronic medium or machine-readable form without the prior written
 * consent of Samsung Electronics.
 *
 * Samsung Electronics makes no representations with respect to the contents,
 * and assumes no responsibility for any errors that might appear in the
 * software and documents. This publication and the contents hereof are subject
 * to change without notice.
 */

#ifndef MDM_SAMSUNG_REMOTEDESKTOP_H
#define MDM_SAMSUNG_REMOTEDESKTOP_H

#include <sys/types.h>
#include <android/rect.h>

// WTL_EDM

/**
 *  Remote Desktop Module provides means by which the admin capture the device screen content.
 *  <p>
 *  It provides the following features for EDM client<br>
 *         - Access to screen content through shared memory from Surface Flinger. <br>
 *           (Note: Surface Flinger is android's composition engine that composes the
 *            UI content and updates to the display<br>
 *         - or Access to screen content through Read-only access to the framebuffer device.<br>
 *         - Screen Changed Notifications<br>
 *           ( This prevents the EDM client from having to poll the screen content continuously 
 *             to check for changes. The notifications shall be received for both approaches fb0 
 *             and surface flinger.)<br>
 *         - Dirty Region feedback after capture<br>
 *  <p>
 *  The decision to use framebuffer device (fb0) or Surface Flinger mechanism is made based on the 
 *  following aspects<br>
 *		-    Availability of the approach <br>
 *			 (Eg: - Only Surface Flinger approach works in some devices as the frame buffer
 *            device is not accessible/available),<br>
 *		-    Frame rate yielded by individual approach and <br>
 *		-    Accuracy of the screen content <br>
 *			   (Eg: - FB0 misses some overlay content. Lets say, Wallpaper is present in one 
 *             layer, Status Bar present in one layer, remaining UIs present in another layer). <br>
 *           Note:  This decision is done for every device at build time.<br>
 *  <p>
 *  The EDM client can register a listener to receive notifications when the screen changes.
 *  On occurrence of screen change, the Remote Desktop module sends a screenChanged callback and 
 *  waits for the capture request from the client. 
 *  (Note: Subsequent screen changes are detected, but they are not notified to the client until 
 *   the capture request for the outstanding callback is received.)<br>
 *  <br>
 *  In case of Remote Desktop session<br>
 *        &nbsp;&nbsp;1) If Remote Desktop Session is started at owner space<br>
 *           &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;a) It can capture owner space screens<br>
 *           &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;b) It can capture Android guest user's screens<br>
 *
 *        &nbsp;&nbsp;2) If Remote Desktop Session is started at Android guest user<br>
 *           &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;a) It can't capture owner space screens<br>
 *           &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;b) It can capture its own user space screens<br>
 *
 *
 *  Events injected by Remote Injection are not injected into user space where Remote Desktop
 *  is disabled.
 *  <br>
 * @policygroup Remote Control
 * @since API level 3
 * @since MDM 2.1
 * @permission  This policy requires the caller to have the
 * "com.samsung.android.knox.permission.KNOX_REMOTE_CONTROL" permission
 * which has a protection level of signature.
 */

namespace knoxremotedesktop {

/**
 * This enum provides the different file descriptor types. 
 * <p>
 * File Descriptor types depend on the underneath capture mechanism.
 * (framebuffer device (fb0) or Surface Flinger mechanism)
 * <br>
 *
 * @since API level 3
 * @since MDM 2.1
 */
typedef enum {
	FD_INVALID, FD_DEV_FB0,        // FD returned from framebuffer device
	FD_SHARED_MEM  // FD returned from shared memory in case of surface flinger.
} FDType;

/**
 * This enum provides the different pixel formats for the device screen. 
 * <p>
 * Pixel formats depend on the support from underneath capture mechanism
 * (framebuffer device (fb0) or Surface Flinger mechanism)<br><br>	
 *  1.  In Surface flinger based capture, RGBA_8888 will be the output format.<br><br>
 *  2.	In FB0 based capture, formats BGRA_8888 and RBG_565 are so far supported
 *      because these were the known formats in so far tested devices.
 *      If the FB0 device provides content in any other format than these formats in 
 *      future devices, then it would be marked as PF_UNKNOWN. But still the EDM 
 *      client can use ioctl directly like below to fetch the colour offset 
 *      information:<br>
 *      <pre>
 *	    remoteDesktop->getScreenInfo(&width, &height, &bytesPerPixel, &pixelFormat);
 *		if (mFDType == FD_DEV_FB0 && pixelFormat == PF_UNKNOWN) {
 *			// Special Case
 *			struct fb_var_screeninfo var;
 *			if(ioctl(mFD, FBIOGET_VSCREENINFO, &var) < 0) {
 *				LOGE("Failed FBIOGET_VSCREENINFO");
 *				return;
 *			}
 *			//The following information shall be used to render the buffer
 *			int redoffset = var.red.offset;
 *			int redlength = var.red.length;
 *			int greenoffset = var.green.offset;
 *			int greenlength = var.green.length;
 *			int blueoffset = var.blue.offset;
 *			int bluelength = var.blue.length;
 *			int alphaoffset = var.transp.offset;
 *			int alphalength = var. transp.length;
 *
 *		   // Code to read the buffer in this special case using above color offset information
 *		} else {
 *			// Code to read the buffer in normal case
 *		}
 *      </pre>
 * <br>
 *
 * @since API level 3
 * @since MDM 2.1
 */
enum {
	PF_UNKNOWN, PF_RGB_565, PF_BGRA_8888, PF_RGBA_8888
};

/**
 * This structure provides the pixel format information in terms of actual component offsets.
 * <p>
 * PixelFormatDetail contains the information about the actual red, green, blue and alpha offset
 * positions and lengths.
 *
 * @since API level 3
 * @since MDM 2.1
 */
typedef struct {
	/** Pixel Size in bits
	 * @since API level 3
	 * @since MDM 2.1
	 */
	int bpp;
	/** Beginning of the red component
	 * @since API level 3
	 * @since MDM 2.1
	 */
	int redoffset;
	/** Length of the red component in bits
	 * @since API level 3
	 * @since MDM 2.1
	 */
	int redlength;
	/** Beginning of the green component
	 * @since API level 3
	 * @since MDM 2.1
	 */
	int greenoffset;
	/** Length of the green component in bits
	 * @since API level 3
	 * @since MDM 2.1
	 */
	int greenlength;
	/** Beginning of the blue component
	 * @since API level 3
	 * @since MDM 2.1
	 */
	int blueoffset;
	/** Length of the blue component in bits
	 * @since API level 3
	 * @since MDM 2.1
	 */
	int bluelength;
	/** Beginning of the alpha component
	 * @since API level 3
	 * @since MDM 2.1
	 */
	int alphaoffset;
	/** Length of the alpha component in bits
	 * @since API level 3
	 * @since MDM 2.1
	 */
	int alphalength;
} PixelFormatDetail;

/**
 * This structure provides the Dirty Region Information
 * <p>
 * DirtyRegion contains the changed screen region in the captured frame. 
 * This is an array of  ARect where each ARect includes {left, top, right, bottom} 
 * which is defined  by Android NDK header file "\android-ndk-r6\platforms\
 * android-9\arch-x86\usr\include\android\rect.h".  
 *
 * @since API level 3
 * @since MDM 2.1
 */
class DirtyRegion {
public:
	DirtyRegion() :
			dirtyRects(0), numRects(0), maxRects(0) {
	}
	~DirtyRegion() {
	}

public:
	/** Array of Rects
	 * @since API level 3
	 * @since MDM 2.1
	 */
	ARect *dirtyRects; // Array of Rects

	/** Number of Dirty Rects in the Array
	 * @since API level 3
	 * @since MDM 2.1
	 */
	int numRects; // Number of Dirty Rects in the Array

	/** Size of Array
	 * @since API level 3
	 * @since MDM 2.1
	 */
	int maxRects; // Size of Array
};

/**
 * This class provides the interface to get notified when the screen changes.
 * <p>
 * Admin should implement the screenChanged() method to get notifications
 *
 * @since API level 3
 * @since MDM 2.1
 */
class IRemoteDesktopListener {
public:
	virtual ~IRemoteDesktopListener() {
	}

	/** Invoked when screen content changes.
	 *
	 * Admin can use this callback to capture the screen when this callback is invoked.
	 * @see IRemoteDesktop#setListener(IRemoteDesktopListener*)
	 * @see IRemoteDesktop#captureScreen(DirtyRegion&)
	 * @since API level 3
	 * @since  MDM 2.1
	 */
	virtual void screenChanged() = 0;
};

/**
 *
 *  Remote Desktop Module provides means by which the admin capture the device screen content.
 *  <p>
 *  It provides the following features for EDM client<br>
 *         - Access to screen content through shared memory from Surface Flinger. <br>
 *           (Note: Surface Flinger is android's composition engine that composes the
 *            UI content and updates to the display<br>
 *         - or Access to screen content through Read-only access to the framebuffer device.<br>
 *         - Screen Changed Notifications<br>
 *           ( This prevents the EDM client from having to poll the screen content continuously
 *             to check for changes. The notifications shall be received for both approaches fb0
 *             and surface flinger.)<br>
 *         - Dirty Region feedback after capture<br>
 *  <p>
 *  The decision to use framebuffer device (fb0) or Surface Flinger mechanism is made based on the
 *  following aspects<br>
 *		-    Availability of the approach <br>
 *			 (Eg: - Only Surface Flinger approach works in some devices as the frame buffer
 *            device is not accessible/available),<br>
 *		-    Frame rate yielded by individual approach and <br>
 *		-    Accuracy of the screen content <br>
 *			   (Eg: - FB0 misses some overlay content. Lets say, Wallpaper is present in one
 *             layer, Status Bar present in one layer, remaining UIs present in another layer). <br>
 *           Note:  This decision is done for every device at build time.<br>
 *  <p>
 *  The EDM client can register a listener to receive notifications when the screen changes.
 *  On occurrence of screen change, the Remote Desktop module sends a screenChanged callback and
 *  waits for the capture request from the client.
 *  (Note: Subsequent screen changes are detected, but they are not notified to the client until
 *   the capture request for the outstanding callback is received.)<br>
 *  <br>
 *  In case of Remote Desktop session<br>
 *        &nbsp;&nbsp;1) If Remote Desktop Session is started at owner space<br>
 *           &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;a) It can capture owner space screens<br>
 *           &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;b) It can capture Android guest user's screens<br>
 *
 *        &nbsp;&nbsp;2) If Remote Desktop Session is started at Android guest user<br>
 *           &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;a) It can't capture owner space screens<br>
 *           &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;b) It can capture its own user space screens<br>
 *
 *
 *  Events injected by Remote Injection are not injected into user space where Remote Desktop
 *  is disabled.
 *  <br>
 * @policygroup Remote Control
 * @since API level 3
 * @since MDM 2.1
 * @permission  This policy requires the caller to have the
 * "com.samsung.android.knox.permission.KNOX_REMOTE_CONTROL" permission
 * which has a protection level of signature.
 */
class IRemoteDesktop {
public:
	virtual ~IRemoteDesktop() {
	}
	;

	/** Returns an instance of a RemoteDesktop client.
	 *
	 * Admin can use this API to initialize the remote desktop session
	 * establishing the connection to the underneath capture mechanism.
	 * <p>
	 * Example client call:<p>
	 * <pre>
	 *  IRemoteDesktop *remoteDesktop = RemoteDesktop::getInstance();
	 *  bool ret = remoteDesktop->init();
	 *  if(ret == false)  { // failure due to either permission
	 *                      // denied or other initialization failures
	 *     // failure code
	 *  } else {
	 *     // success code
	 *  }
	 * </pre>
	 * @return  A remote desktop instance if successful, else <code>null</code>.
	 * @since API level 3
	 * @since  MDM 2.1
	 */
	static IRemoteDesktop *getInstance();

	/**
	 * API to initialize remote desktop session.
	 *
	 * @usage Admin can use this API to initialize the remote desktop session
	 * establishing the connection to the underneath capture mechanism.
	 * <p>
	 * Example client call:<p>
	 * <pre>
	 *  IRemoteDesktop *remoteDesktop = RemoteDesktop::getInstance();
	 *  bool ret = remoteDesktop->init();
	 *  if(ret == false)  { // failure due to either permission 
	 *                      // denied or other initialization failures
	 *     // failure code
	 *  } else {
	 *     // success code
	 *  }
	 * </pre>
	 *
	 * @permission  The use of this API requires the caller to have the
	 * "com.samsung.android.knox.permission.KNOX_REMOTE_CONTROL" permission which has a protection level
	 * of signature.
	 *
	 * @return  <code>true</code> if successfully initialised, else <code>false</code>.
	 * @since API level 3
	 * @since  MDM 2.1
	 * @see #getInstance()
	 * @see #getScreenInfo(int*, int*, int*, int*)
	 * @see #getFrameBufInfo(int *, FDType *)
	 * @see #setListener(IRemoteDesktopListener*)
	 * @see #captureScreen(DirtyRegion&)
	 *
	 */
	virtual bool init() = 0;

	/**
	 * API to get Frame Buffer's Resolution/PixelFormat information.
	 *
	 * @usage Admin can use this API to get the framebuffer dimensions, pixel formats and
	 * pixel byte size.
	 *
	 * Note: PixelFormat returned by this API is not reliable. It will not give accurate information
	 * on the exact position details of each pixel. Also for FB0 FDType, the pixel format may also be returned
	 * as PF_UNKNOWN. It is recommended that client always rely on the API getScreenPixelFormatInfo().
	 *
	 * <p>
	 * Example client call:<p>
	 * <pre>
	 *  IRemoteDesktop *remoteDesktop = RemoteDesktop::getInstance();
	 *  bool ret = remoteDesktop->init();
	 *  if(ret == false)  {
	 *     // failure code
	 *  } else {
	 *     // success code	 
	 *     int	width, height, pixelFormat, bytesPerPixel;	 
	 *     remoteDesktop->getScreenInfo(&width, &height, &bytesPerPixel, &pixelFormat);
	 *  }
	 * </pre>
	 *
	 * @permission  The use of this API requires the caller to have the
	 * "com.samsung.android.knox.permission.KNOX_REMOTE_CONTROL" permission which has a protection level
	 * of signature.
	 *
	 * @param width
	 *           Screen Width
	 * @param height
	 *           Screen Height
	 * @param bytesPerPixel
	 *           Pixel Size in Bytes
	 * @param pixelFormat
	 *           Pixel Format.
	 * @return  <code>true</code> if screen information successfully retrieved, else <code>false</code>.
	 * @since API level 3
	 * @since  MDM 2.1
	 * @see #getInstance()
	 * @see #init()
	 * @see #getFrameBufInfo(int *, FDType *)
	 * @see #setListener(IRemoteDesktopListener*)
	 * @see #captureScreen(DirtyRegion&)
	 *
	 */
	virtual void getScreenInfo(int *width, int *height, int *bytesPerPixel,
			int *pixelFormat) = 0;

	/**
	 * API to get Frame Buffer Information.
	 *
	 * @usage Admin can use this API to get File Descriptor to
	 * framebuffer device or Shared memory.
	 * <p>
	 * Example client call:<p>
	 * <pre>
	 *  IRemoteDesktop *remoteDesktop = RemoteDesktop::getInstance();
	 *  bool ret = remoteDesktop->init();
	 *  if(ret == false)  {
	 *     // failure code
	 *  } else {
	 *     // success code	 
	 *     int	     mFD;
	 *     FDType 	 mFDType;
	 *     bool err = remoteDesktop->getFrameBufInfo(&mFD, &mFDType);
	 *     if(err == false)  {
	 *         // failure code
	 *     } else {
	 *         // success code	 
	 *     }
	 *  }
	 * </pre>
	 *
	 * @permission  The use of this API requires the caller to have the
	 * "com.samsung.android.knox.permission.KNOX_REMOTE_CONTROL" permission which has a protection level
	 * of signature.
	 *
	 * @param fd
	 *           File Descriptor
	 * @param fdType
	 *           File Descriptor Type (Shared Memory or Frame Buffer)
	 * @return  <code>true</code> if frame buffer information successfully retrieved, else <code>false</code>.
	 * @since API level 3
	 * @since  MDM 2.1
	 * @see #getInstance()
	 * @see #init()
	 * @see #getScreenInfo(int*, int*, int*, int*)
	 * @see #setListener(IRemoteDesktopListener*)
	 * @see #captureScreen(DirtyRegion&)
	 */
	virtual bool getFrameBufInfo(int *fd, FDType *fdType) = 0;

	/**
	 * API to register Screen Event listener
	 *
	 * @usage Admin can use this API to get Screen Changed Event Callbacks
	 * from remote desktop module.
	 * <p>
	 * Example client call:<p>
	 * <pre>
	 *  class RemoteDesktopListener : public IRemoteDesktopListener {
	 *      RemoteDesktopListener() {}
	 *      virtual void screenChanged() {
	 *	        LOGI("RemoteDesktopListener :: screenChanged()");
	 *          // Capture Screen Request code
	 *      }
	 *  }
	 *
	 *  ....
	 *
	 *  IRemoteDesktop *remoteDesktop = RemoteDesktop::getInstance();
	 *  bool ret = remoteDesktop->init();
	 *  if(ret == false)  {
	 *     // failure code
	 *  } else {
	 *     // success code	 
	 *     IRemoteDesktopListener *remoteDesktoplistener = new RemoteDesktopListener();
	 *     bool err = remoteDesktop->setListener(remoteDesktoplistener);
	 *     if(err == false)  {
	 *         // failure code
	 *     } else {
	 *         // success code
	 *     }
	 *  }
	 * </pre>
	 *
	 * @permission  The use of this API requires the caller to have the
	 * "com.samsung.android.knox.permission.KNOX_REMOTE_CONTROL" permission which has a protection level
	 * of signature.
	 *
	 * @param listener
	 *           Screen Change Event Listener
	 * @return  <code>true</code> if listener is successfully set, else <code>false</code>.
	 * @since API level 3
	 * @since  MDM 2.1
	 * @see #getInstance()
	 * @see #init()
	 * @see #getScreenInfo(int*, int*, int*, int*)
	 * @see #getFrameBufInfo(int *, FDType *)
	 * @see #captureScreen(DirtyRegion&)
	 */
	virtual bool setListener(IRemoteDesktopListener *listener) = 0;

	/**
	     * API to capture the screen.
	     *
	     * Admin can use this API to capture the screen. Remote Desktop
		 * module returns the changed region or dirty region in response
		 * to this request.  (This API updates the screen contents to shared memory
		 * if fdType == FD_SHARED_MEM.)<br>
	     * [NOTE] It is recommended to follow the following sequence:<br>
	     * Step 1 : Call IRemoteDesktop::getInstance() to get an instance of IRemoteDesktop<br>
	     * Step 2 : Call IRemoteDesktop::setScreenInfo (prefW, prefH, prefFormat) to set the desired screen width,height and pixel format<br>
	     * Step 3 : Call IRemoteDesktop::init()<br>
	     * Step 4 : Call IRemoteDesktop::getFrameBufInfo() to get file descriptor and type of it<br>
	     * Step 5 : Call mmap() to map file descriptor to memory<br>
	     * Step 6 : Set screen update listener<br>
	     * Step 7 : Call IRemoteDesktop::captureScreen() to get dirty regions after screenChanged() callback is called<br>
	     * <p>
	     * Example client call:<p>
	     * <pre>
	     * class RemoteDesktopListener : public IRemoteDesktopListener {
	     * 	 RemoteDesktopListener() {}
	     * 	 virtual void screenChanged() {
	     * 		 LOGI("RemoteDesktopListener :: screenChanged()");
	     *		   	 // Capture Screen Request code
	     *			 DirtyRegion dirtyRegion;
	     *			 bool err = remoteDesktop->captureScreen(dirtyRegion);
	     *			 if(err == false)  {
	     *				 // failure code
	     *			} else {
	     *				 // success code
	     *			}
	     *		  }
	     *	   }
	     *	 ....
	     *
	     *	 IRemoteDesktop *remoteDesktop = RemoteDesktop::getInstance();
	     *	 int w = 360, h = 640, format = PF_RGBA_8888;
	     *	 int	  mFD;
	     *	 FDType   mFDType;
	     *	 bool ret;
	     *
	     *	 ret = remoteDesktop->setScreenInfo(w,h,format);
	     *	 if(ret == false){
	     *		 // failure code
	     *	 }
	     *
	     *	 ret = remoteDesktop->init();
	     *	 if(ret == false){
	     *		 // failure code
	     *	 }
	     *
	     *	 ret = remoteDesktop->getFrameBufInfo(&mFD, &mFDType);
	     *	 if(ret == false){
	     *		 // failure code
	     *	 }
	     *
	     *	 if (mFDType == FD_DEV_FB0) {
	     *		 struct fb_var_screeninfo vinfo;
	     *		 if(ioctl(mFD, FBIOGET_VSCREENINFO, &vinfo) < 0) return false;
	     *		 int offset = vinfo.xoffset * mBytesPerPixel + vinfo.xres * vinfo.yoffset * mBytesPerPixel;
	     *		 mFrame = (unsigned short *) mmap(0, mFrameBufferSize, PROT_READ, MAP_PRIVATE, mFD, offset);
	     *	 } else if (mFDType == FD_SHARED_MEM) {
	     *		 mFrame = (unsigned short *) mmap(0, mFrameBufferSize, PROT_READ, MAP_SHARED, mFD, 0);
	     *	 } else {
	     *		 LOGE("mapFrameBuffer Failed");
	     *		 return false;
	     *	 }
	     *
	     *	 IRemoteDesktopListener *remoteDesktoplistener = new RemoteDesktopListener();
	     *	 remoteDesktop->setListener(remoteDesktoplistener);
	     * </pre>
	     *
	     * @permission  The use of this API requires the caller to have the
	     * "com.samsung.android.knox.permission.KNOX_REMOTE_CONTROL" permission which has a protection level
	     * of signature.
	     *
	     * @param region
	     *           Dirty Region Information
	     * @return  <code>true</code> if captured successfully, else <code>false</code>.
	     * @since API level 3
	     * @since  MDM 2.1
	     * @see #getInstance()
	     * @see #init()
		 * @see #getScreenInfo(int*, int*, int*, int*)
		 * @see #getFrameBufInfo(int *, FDType *)
	     */
	virtual bool captureScreen(DirtyRegion &region) = 0;

	/**
	 * API to get screen pixel format information.
	 *
	 * @usage Admin can use this API to get the pixel format detailed information.
	 * <p>
	 * Example client call:<p>
	 * <pre>
	 *  IRemoteDesktop *remoteDesktop = RemoteDesktop::getInstance();
	 *  bool ret = remoteDesktop->init();
	 *  if(ret == false)  {
	 *     // failure code
	 *  } else {
	 *     // success code
	 *     PixelFormatDetail formatDetail;
	 *     remoteDesktop->getScreenPixelFormatInfo(formatDetail);
	 *  }
	 * </pre>
	 *
	 * @permission  The use of this API requires the caller to have the
	 * "com.samsung.android.knox.permission.KNOX_REMOTE_CONTROL" permission which has a protection level
	 * of signature.
	 *
	 * @param formatDetail
	 *           Pixel Format Detail
	 * @return  nothing
	 * @since API level 3
	 * @since  MDM 2.1
	 * @see #getInstance()
	 * @see #init()
	 * @see #getFrameBufInfo(int *, FDType *)
	 * @see #setListener(IRemoteDesktopListener*)
	 * @see #captureScreen(DirtyRegion&)
	 *
	 */
	virtual void getScreenPixelFormatInfo(PixelFormatDetail &formatDetail) = 0;

	///////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////

	/**
	 * API to set the framebuffer's width, height and format preferred by the admin
	 *
	 * @usage Admin can use this API to set the preference for the remote screen dimensions considering the
	 * different factors like network bandwidth usage and performance. This may or may not be honoured based
	 * on the platform support. Mostly in case of FB0, this will not be honoured.
	 *
	 * <p>
	 * Example client call:<p>
	 * <pre>
	 *  int w = 360, h = 640, format = PF_RGB_565;
	 *  IRemoteDesktop *remoteDesktop = RemoteDesktop::getInstance();
	 *  bool ret = remoteDesktop->setScreenInfo(w,h,format);
	 *  if(ret == false)  { // failure due to either permission 
	 *                      // denied or other initialization failures
	 *     // failure code
	 *  } else {
	 *     // success code
	 *  }
	 * </pre>
	 *
	 * @permission  The use of this API requires the caller to have the
	 * "com.samsung.android.knox.permission.KNOX_REMOTE_CONTROL" permission which has a protection level
	 * of signature.
	 *
	 * @param prefW
	 *           Preferred Screen Width
	 * @param prefH
	 *           Preferred Screen Height
	 * @param prefFormat
	 *           Preferred Pixel Format
	 *
	 * @return  <code>true</code> if success, else <code>false</code>.
	 * @since API level 3
	 * @since  MDM 2.1
	 * @see #getInstance()
	 * @see #getScreenInfo(int*, int*, int*, int*)
	 * @see #getFrameBufInfo(int *, FDType *)
	 * @see #setListener(IRemoteDesktopListener*)
	 * @see #captureScreen(DirtyRegion&)
	 *
	 */
	virtual bool setScreenInfo(int prefW, int prefH, int prefFormat) = 0;

	/**
	 * API to get the default screen information.
	 *
	 * @usage Admin can use this API to get the default hardware screen dimensions, pixel formats and
	 * pixel byte size. This new API is introduced as getScreenInfo need not return the default screen
	 * information as it returns the selected framebuffer information based on the preferences
	 * set by the admin
	 *
	 * <p>
	 * Example client call:<p>
	 * <pre>
	 *  IRemoteDesktop *remoteDesktop = RemoteDesktop::getInstance();
	 *  bool ret = remoteDesktop->init();
	 *  if(ret == false)  {
	 *     // failure code
	 *  } else {
	 *     // success code	 
	 *     int	hwwidth, hwheight, hwpixelFormat, hwbytesPerPixel;	
	 *     remotedesktop->getDefaultScreenInfo(&hwwidth, &hwheight, &hwbytesPerPixel, &hwpixelFormat);
	 *  }
	 * </pre>
	 *
	 * @permission  The use of this API requires the caller to have the
	 * "com.samsung.android.knox.permission.KNOX_REMOTE_CONTROL" permission which has a protection level
	 * of signature.
	 *
	 * @param hwwidth
	 *           Hardware Screen Width
	 * @param hwHeight
	 *           Hardware Screen Height
	 * @param hwbytesPerPixel
	 *           Hardware Pixel Size in Bytes
	 * @param hwpixelFormat
	 *           Hardware Pixel Format.
	 * @return  nothing
	 * @since API level 3
	 * @since  MDM 2.1
	 * @see #getInstance()
	 * @see #init()
	 * @see #getFrameBufInfo(int *, FDType *)
	 * @see #setListener(IRemoteDesktopListener*)
	 * @see #captureScreen(DirtyRegion&)
	 *
	 */
	virtual void getDefaultScreenInfo(int *hwwidth, int *hwheight,
			int *hwbytesPerPixel, int *hwpixelFormat) = 0;

};

} //namespace knoxremotedesktop

#endif // MDM_SAMSUNG_REMOTEDESKTOP_H
