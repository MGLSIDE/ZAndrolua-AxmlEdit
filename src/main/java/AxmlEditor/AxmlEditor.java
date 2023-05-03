package AxmlEditor;

import AxmlEditor.rt.NodeVisitor;
import AxmlEditor.rt.Reader;
import AxmlEditor.rt.Util;
import AxmlEditor.rt.Visitor;
import AxmlEditor.rt.Writer;
import AxmlEditor.fix.EntryPoint;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import android.util.Log;

public class AxmlEditor {
    private final String[] components = {"activity", "activity-alias", "provider", "receiver", "service","intent-filter","data"};
    private File mManifest;
    private int mVersionCode = -1;
    private int mMinimumSdk = -1;
    private int mTargetSdk = -1;
    private int mCompileSdkVersion = -1;
    private int mPlatformBuildVersionCode = -1;
    private int mMaxSdk = -1;
    private int mInstallLocation = -1;
    private String mVersionName;
    private String mAppName;
    private String mPackageName;
    private byte[] mManifestData;
    private String[] permissions;
    private boolean changeHost=true;
    private boolean ExtractNativeLibs;
    private boolean ModifyExtractNativeLibs;
    private String targetScheme;
    private String compileSdkVersionCodename;
    private String replaceScheme;
    private String[] providerKeys;
    private String[] providerRaws;
    private HashMap<String,String> pathPattern;
    private boolean usefix=false;
    private String[] providerNews;
    private HashMap<String,String> ReplaceActivitys;
    private String PlatformBuildVersionName;

    public AxmlEditor(String manifest) {
        mManifest = new File(manifest);
        ReplaceActivitys=new HashMap<String,String>();
    }
    public AxmlEditor(File manifest) {
        mManifest = manifest;
        ReplaceActivitys=new HashMap<String,String>();
    }
    public AxmlEditor(InputStream manifest) throws IOException {
        mManifestData = Util.readIs(manifest);
        ReplaceActivitys=new HashMap<String,String>();
    }
    public AxmlEditor(byte[] manifest) {
        mManifestData = manifest;
        ReplaceActivitys=new HashMap<String,String>();
    }
    public void setUseFix(boolean use) {
        usefix = use;
    }
    public void setExtractNativeLibs(boolean extractNativeLibs) {
        ExtractNativeLibs = extractNativeLibs;
        ModifyExtractNativeLibs = true;
    }
    public void setPathPattern(String activity, String PathPatterns) {
        if (pathPattern == null) {
            pathPattern = new HashMap<String,String>();
        }
        pathPattern.put(activity, PathPatterns);
    }
    public void setChangeScheme(String target, String new_scheme) {
        targetScheme = target;
        replaceScheme = new_scheme;
    }
    public void setChangeHostIfLikePackageName(boolean change) {
        changeHost = change;
    }
    public void setUsePermissions(String[] list) {
        permissions = list;
    }
    public void setVersionCode(int versionCode) {
        mVersionCode = versionCode;
    }
    public void setVersionName(String versionName) {
        mVersionName = versionName;
    }
    public void setAppName(String appName) {
        mAppName = appName;
    }
    public void setPlatformBuildVersionCode(int PlatformBuildVersionCode) {
        mPlatformBuildVersionCode = PlatformBuildVersionCode;
    }
    public void setPlatformBuildVersionName(String platformBuildVersionName) {
        PlatformBuildVersionName = platformBuildVersionName;
    }
    public void setCompileSdkVersion(int CompileSdkVersion) {
        mCompileSdkVersion = CompileSdkVersion;
    }
    public void setCompileSdkVersionCodename(String compileSdkVersionCodenames) {
        compileSdkVersionCodename = compileSdkVersionCodenames;
    }
    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }
    public void setMinimumSdk(int sdk) {
        mMinimumSdk = sdk;
    }
    public void setTargetSdk(int sdk) {
        mTargetSdk = sdk;
    }
    public void setMaxSdk(int sdk) {
        mMaxSdk = sdk;
    }
    public void setInstallLocation(int location) {
        mInstallLocation = location;
    }
    public void setTargetReplaceAppName(String name) {
        needAutoGuessTarget = false;
        targetReplaceAppName = name;
    }
    
    public void ReplaceActivity(String ReplacedActy,String ReplaceActy){
        ReplaceActivitys.put(ReplacedActy,ReplaceActy);
    }
    
    public void setTargetReplacePackageName(String name) {
        needAutoGuessTarget = false;
        targetReplacePackageName = name;
    }
    public void setProviderHandleTask(String[] keys, String[] raws, String[] news) {
        providerKeys = keys;
        providerRaws = raws;
        providerNews = news;
    }

    private boolean permissionLoopOK=false;
    private String targetReplaceAppName;
    private String targetReplacePackageName;
    private boolean needAutoGuessTarget=true;
    /*private LuaActivity a;
     public void MD(LuaActivity a){
     this.a=a;
     }/**/
    public void commit() throws IOException {
        Reader reader = new Reader(mManifestData == null ?Util.readFile(mManifest): mManifestData);
        Writer writer = new Writer();
        permissionLoopOK = false;
        if (needAutoGuessTarget) {
            targetReplaceAppName = null;
            targetReplacePackageName = null;
        }
        reader.accept(new Visitor(writer) {
                public NodeVisitor child(String ns, String name) {
                    return new NodeVisitor(super.child(ns, name)) {
                        public NodeVisitor child(String ns, String name) {
                            //只要你保留一个uses-permission的tag我就给你批量加标签
                            if (name.equalsIgnoreCase("uses-permission")) {
                                /*if(false){
                                 return new NodeVisitor(super.child(ns, name)) {
                                 @Override
                                 public void attr(String ns,String name,int resourceId,int type,Object value) {
                                 a.sendMsg(ns+"|"+ name+"|"+ resourceId+"|"+type+"|"+value);
                                 }
                                 };}*/

                                if (permissions == null) {
                                    return super.child(ns, name);
                                }
                                if (!permissionLoopOK) {                               
                                    for (String permission:permissions) {
                                        super.child(ns, name).attr("http://schemas.android.com/apk/res/android", "name", 16842755, 3, permission);
                                    }
                                    permissionLoopOK = true;
                                }
                                return null;
                            } else if (name.equalsIgnoreCase("uses-sdk")) {
                                return new NodeVisitor(super.child(ns, name)) {
                                    @Override
                                    public void attr(String ns, String name, int resourceId, int type, Object value) {
                                        if (name.equalsIgnoreCase("minSdkVersion") && mMinimumSdk > 0) {
                                            value = mMinimumSdk;
                                            type = TYPE_FIRST_INT;
                                        } else if (name.equalsIgnoreCase("targetSdkVersion") && mTargetSdk > 0) {
                                            value = mTargetSdk;
                                            type = TYPE_FIRST_INT;
                                        } else if (name.equalsIgnoreCase("maxSdkVersion") && mMaxSdk > 0) {
                                            value = mMaxSdk;
                                            type = TYPE_FIRST_INT;
                                        }
                                        super.attr(ns, name, resourceId, type, value);
                                    }
                                };
                            } else if (name.equalsIgnoreCase("application")) {
                                return new NodeVisitor(super.child(ns, name)) {
                                    public NodeVisitor child(String ns, String name) {
                                        if (name.equalsIgnoreCase("activity")) {
                                            return new NodeVisitor(super.child(ns, name)) {
                                                private String CurrentActivity;
                                                @Override
                                                public void attr(String ns, String name, int resourceId, int type, Object value) {
                                                    if (name.equalsIgnoreCase("label") && mAppName != null && value.equals(targetReplaceAppName)) {
                                                        value = mAppName;
                                                        type = TYPE_STRING;
                                                    }
                                                    
                                                    if (name.equalsIgnoreCase("name")) {
                                                        CurrentActivity = (String)value;    
                                                        if(ReplaceActivitys.get(CurrentActivity)!=null){
                                                            value=ReplaceActivitys.get(CurrentActivity);
                                                        }
                                                    }

                                                    super.attr(ns, name, resourceId, type, value);
                                                }

                                                public NodeVisitor child(String ns, String name) {
                                                    if (name.equalsIgnoreCase("intent-filter") && targetReplacePackageName != null && mPackageName != null) {
                                                        return new NodeVisitor(super.child(ns, name)) {
                                                            public NodeVisitor child(String ns, String name) {
                                                                if (name.equalsIgnoreCase("data")) {
                                                                    return new NodeVisitor(super.child(ns, name)) {
                                                                        @Override
                                                                        public void attr(String ns, String name, int resourceId, int type, Object value) {

                                                                            if (name.equalsIgnoreCase("host") && changeHost && value.equals(targetReplacePackageName)) {
                                                                                value = mPackageName;
                                                                                type = TYPE_STRING;
                                                                            } else if (name.equalsIgnoreCase("scheme") && targetScheme != null && replaceScheme != null) {
                                                                                value = replaceScheme;
                                                                                type = TYPE_STRING;
                                                                            } else if (name.equalsIgnoreCase("pathPattern") && pathPattern != null) {
                                                                                if (pathPattern.get(CurrentActivity) != null) {
                                                                                    value = pathPattern.get(CurrentActivity);
                                                                                    type = TYPE_STRING;
                                                                                }
                                                                            }

                                                                            super.attr(ns, name, resourceId, type, value);
                                                                        }
                                                                    };
                                                                }
                                                                return super.child(ns, name);
                                                            };
                                                        };
                                                    }
                                                    return super.child(ns, name);
                                                };
                                            };
                                        }

                                        for (String component : components) {
                                            if (name.equalsIgnoreCase(component)) {
                                                final String innerTag=component;
                                                return new NodeVisitor(super.child(ns, name)) {
                                                    @Override
                                                    public void attr(String ns, String name, int resourceId, int type, Object value) {
                                                        if (name.equalsIgnoreCase("name") && value instanceof String && mPackageName != null) {
                                                            int check = ((String) value).indexOf(".");
                                                            if (check < 0) {
                                                                value = mPackageName + "." + value;
                                                            } else if (check == 0) {
                                                                value = mPackageName + value;
                                                            }
                                                            type = TYPE_STRING;
                                                        } else if (innerTag.equalsIgnoreCase("provider") && name.equalsIgnoreCase("authorities") && mPackageName != null && targetReplacePackageName != null) {
                                                            if (targetReplacePackageName.equals(value)) { 
                                                                value = mPackageName;
                                                                type = TYPE_STRING;
                                                            }
                                                        }
                                                        //单独设置的事件，以他为准
                                                        if (innerTag.equalsIgnoreCase("provider") && providerKeys != null && providerNews != null && providerRaws != null) {
                                                            for (int i=0;i < providerKeys.length;i++) {                                              
                                                                if (name.equalsIgnoreCase(providerKeys[i])) {
                                                                    if (value.equals(providerRaws[i])) {
                                                                        value = providerNews[i];
                                                                        type = TYPE_STRING;
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        super.attr(ns, name, resourceId, type, value);
                                                    }
                                                };
                                            }
                                        }
                                        return super.child(ns, name);
                                    }
                                    @Override
                                    public void attr(String ns, String name, int resourceId, int type, Object value) {
                                        if (targetReplaceAppName == null) {
                                            targetReplaceAppName = String.valueOf(value);
                                        }
                                        if (name.equalsIgnoreCase("label") && mAppName != null) {
                                            value = mAppName;
                                            type = TYPE_STRING;
                                        } else if ((name.equalsIgnoreCase("extractNativeLibs") && ModifyExtractNativeLibs)) {
                                            value = ExtractNativeLibs;
                                            type = TYPE_INT_BOOLEAN;
                                            //return;
                                        }
                                        super.attr(ns, name, resourceId, type, value);
                                    }
                                };
                            }
                            return super.child(ns, name);
                        }

                        @Override
                        public void attr(String ns, String name, int resourceId, int type, Object value) {
                            if (name.equalsIgnoreCase("package") && mPackageName != null) {
                                targetReplacePackageName = String.valueOf(value);
                                value = mPackageName;
                                type = TYPE_STRING;
                            } else if (name.equalsIgnoreCase("installLocation")) {
                                int loc = getRealInstallLocation(mInstallLocation);
                                if (loc >= 0) {
                                    value = loc;
                                    type = TYPE_FIRST_INT;
                                } else {
                                    return;
                                }
                            } else if (name.equalsIgnoreCase("versionName") && mVersionName != null) {
                                value = mVersionName;
                                type = TYPE_STRING;
                            } else if (name.equalsIgnoreCase("versionCode") && mVersionCode > 0) {
                                value = mVersionCode;
                                type = TYPE_FIRST_INT;
                            } else if (name.equalsIgnoreCase("platformBuildVersionName") && (PlatformBuildVersionName != null)) {
                                value = PlatformBuildVersionName;
                                type = TYPE_STRING;
                            } else if (name.equalsIgnoreCase("PlatformBuildVersionCode") && mPlatformBuildVersionCode > 0) {
                                value = mPlatformBuildVersionCode;
                                type = TYPE_FIRST_INT;
                            } else if (name.equalsIgnoreCase("compileSdkVersionCodename") && (compileSdkVersionCodename != null)) {
                                value = compileSdkVersionCodename ;
                                type = TYPE_STRING;
                            } else if (name.equalsIgnoreCase("compileSdkVersion") && mCompileSdkVersion > 0) {
                                value = mCompileSdkVersion;
                                type = TYPE_FIRST_INT;
                            }

                            super.attr(ns, name, resourceId, type, value);
                        }
                    };
                }
            });
        mManifestData = usefix ? EntryPoint.fix(writer.toByteArray()): writer.toByteArray();
    }
    public void writeTo(FileOutputStream manifestOutputStream) throws IOException {
        manifestOutputStream.write(mManifestData);
        manifestOutputStream.close();
    }

    public void writeTo(OutputStream manifestOutputStream) throws IOException {
        manifestOutputStream.write(mManifestData);
    }

    /*
     Return real install location from selected item in spinner
     */
    private int getRealInstallLocation(int installLocation) {
        switch (installLocation) {
            case 0:
                return -1;//default
            case 1:
                return 0;//auto
            case 2:
                return 1;//internal
            case 3:
                return 2;//external
            default:
                return -1;
        }
    }
}
