# osm2pgrouting使用

osm2pgrouting是一个将osm格式的数据导入到PostgreSQL中空间数据库的工具。

osm2pgrouting可以在Windows和Linux两种系统环境下使用，Linux环境的安装方法见[osm2pgrouting的github]( https://github.com/pgRouting/osm2pgrouting )。本文重点介绍Windows环境下的安装，osm2pgrouting的github网站托管了osm2pgrouting的源码，但是这些源码在Windows环境下编译十分困难，而且包括很多依赖。PostGis的Windows安装包帮助我们把这些源码编译成了可执行文件，所以可以通过PostGis安装osm2pgrouting。

## 1. 安装PostGis

PostGis的安装前文有过介绍，可以参考[文档](https://github.com/Konfuse/TR-Flink/blob/master/doc/PgRouting%E7%8E%AF%E5%A2%83%E6%90%AD%E5%BB%BA.md#2-%E5%A6%82%E4%BD%95%E5%AE%89%E8%A3%85pgrouting)。

## 2. 配置环境变量

PostGis的安装路径默认要求选择在PostgreSQL的目录下，所以PostGis的很多可执行文件也放在了PostgreSQL根目录下的`bin`目录里。

打开电脑环境变量配置，将PostgreSQL根目录下的`bin`目录加入`Path`，例如：`E:\PostgreSQL\12\bin`.

## 3. 如何使用osm2pgrouting

这里给出一个osm2pgrouting的使用范例：

### 3.1 创建一个测试数据库

创建一个名字为`routing`的数据库，并为它创建postgis地理数据库插件与pgrouting路由扩展。

```shell
createdb routing
psql --dbname routing -c 'CREATE EXTENSION postgis'
psql --dbname routing -c 'CREATE EXTENSION pgRouting'
```

### 3.2 导入osm文件

```shell
osm2pgrouting --f your-OSM-XML-File.osm --conf mapconfig.xml --dbname routing --username postgres --clean
```

命令指定了osm文件`your-OSM-XML-File.osm`，与配置文件`mapconfig.xml`，配置文件分为四种，都可以在[osm2pgrouting的github]( https://github.com/pgRouting/osm2pgrouting )上找到。

其它的命令参数，可以通过`osm2pgrouting --help`获取：

```shell
 osm2pgrouting --help
Allowed options:

Help:
  --help                Produce help message for this version.
  -v [ --version ]      Print version string

General:
  -f [ --file ] arg                     REQUIRED: Name of the osm file.
  -c [ --conf ] arg (=/usr/share/osm2pgrouting/mapconfig.xml)
                                        Name of the configuration xml file.
  --schema arg                          Database schema to put tables.
                                          blank: defaults to default schema
                                                dictated by PostgreSQL
                                                search_path.
  --prefix arg                          Prefix added at the beginning of the
                                        table names.
  --suffix arg                          Suffix added at the end of the table
                                        names.
  --addnodes                            Import the osm_nodes, osm_ways &
                                        osm_relations tables.
  --attributes                          Include attributes information.
  --tags                                Include tag information.
  --chunk arg (=20000)                  Exporting chunk size.
  --clean                               Drop previously created tables.
  --no-index                            Do not create indexes (Use when indexes
                                        are already created)

Database options:
  -d [ --dbname ] arg            Name of your database (Required).
  -U [ --username ] arg          Name of the user, which have write access to
                                 the database.
  -h [ --host ] arg (=localhost) Host of your postgresql database.
  -p [ --port ] arg (=5432)      db_port of your database.
  -W [ --password ] arg          Password for database access.
```

