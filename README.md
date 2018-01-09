本项目为IDEA插件源码。

#### 开发初衷
作为java后端的开发者，我们经常需要给各种模型设置值。对只有几个的属性模型，设值还好，而对于有几十个属性的模型设置值的时候，往往需要花费
大量的时间和精力。在读别人的代码时，经常发现某些模型重复设置值，或者少设置值，这往往会出现NPE。当然，如果需要设置的值来自另一个模型，
而且 另一个模型和当前模型有大量的属性名称相同，那么就可以使用第三方库的copyProperties. 否则，就需要我们手动设置值，这往往是最痛苦的。
基于以上原因，我开发了这个插件，旨在帮助开发者自动生成设置属性的代码。

#### 插件功能
自动生成设置属性值的代码.
1. 模型中所有set方法
2. 模型父类中的所有set方法


#### 编译方法
将该项目导入idea, 设置好Plugin sdk环境，点击Build选项下的Prepare Plugin Module xxx 即可生成插件包(一个jar文件)。


#### 安装方法
自己编译生成.jar或.zip文件
或者直接下载本工程里的attributes-set.jar文件
File ==> Settings ==> Plugins ==> Install plugin from disk ==> 选择.jar或者.zip文件

#### 使用方法
![模型1](/imgs/user.png)
![模型2](/imgs/user2.png)
1. 在需要设置属性的实例对象下面 按 alt + insert 键弹出菜单,选择attributesSet选项 或者使用快捷键 alt + S。
![弹出菜单](/imgs/p1.png)
2. 基本模型
![基本模型](/imgs/p2.png)
3. 复杂模型
![复杂模型](/imgs/p3.png)
