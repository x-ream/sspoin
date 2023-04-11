# sspoin 简单的Excel数据导入, 返回错误数据
   
   
[![license](https://img.shields.io/github/license/x-ream/sspoin.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![maven](https://img.shields.io/maven-central/v/io.xream.sspoin/sspoin.svg)](https://search.maven.org/search?q=io.xream)
[![Gitter](https://badges.gitter.im/x-ream/x-ream.svg)](https://gitter.im/x-ream/community)
    
   [WIKI](https://github.com/x-ream/sspoin/wiki)
    


## 使用方法

### 1. 创建模板类
```java
@Template(metaRow = 1,startRow = 2,
        dateFormat = "yyyy-MM-dd",
        blankError = "不能为空",
        zeroError = "不能为0",
        repeatedError = "数据重复",
        existsError = "数据已存在"
)
public class EquipTemplate implements Templated {

    private int rowNum;
    @Template.Row(meta="设备型号")
    private String model;
    @Template.Row(meta="设备条码", nonRepeatable = true)
    private String barCode;
    @Template.Row(meta="RFID", nonRepeatable = true)
    private String rfid;
    @Template.Row(meta="数量")
    private long amount;//数量*
    @Template.Row(meta="出厂日期")
    private Date manuDate;//出厂日期

    private RowError rowError = new RowError();

    @Override
    public int getRowNum() {
        return rowNum;
    }

    @Override
    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBarCode() {
        return barCode;
    }

    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public Date getManuDate() {
        return manuDate;
    }

    public void setManuDate(Date manuDate) {
        this.manuDate = manuDate;
    }

    @Override
    public RowError getRowError() {
        return this.rowError;
    }

}
```

### 2. 在web app里调用
```java
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile file = multipartRequest.getFile("file");
         
        final String fileName = file.getOriginalFilename();
        final Parsed parsed = Parsed.of(EquipTemplate.class);//解析模板类
        final Errors errors = Errors.of(parsed, fileName);
        Result<EquipTemplate> result = null;
        InputStream in = null;
        try {
            in = file.getInputStream();
            result = ExcelReader.read(errors, parsed, fileName, in); //读取数据
            if (result.getList().isEmpty()) {
                return ViewEntity.ok();
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //过滤掉已存在的数据
        NonRepeatableSavingFilter.filter(
                errors,
                parsed,
                result,
                Equipment.class,//实体类
                this.nonRepeatableSavedCond,//查询条件构建, 获得cond
                cond -> this.equipmentFindService.listResultMap((Criteria.ResultMapCriteria) cond)
        );

        // TODO: 写入数据库 result.getList()
        
        // TODO: 返回 errors
```

### 3. 请求下载错误数据excel文件
```java
    @RequestMapping("/download")
    public void download(@RequestBody ErrorsRO ro, HttpServletResponse response) throws Exception {

        String fileName = ro.getFileName();
        if (fileName.endsWith(".xls")) {
            fileName += "x";
        } else if (fileName.endsWith(".XLS")) {
            fileName += "X";
        }

        byte[] buffer = ro.toBuffer();

        response.reset();
        response.setContentType("application/octet-stream;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + new String(("错误数据_" + fileName).getBytes(), "ISO-8859-1"));
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-Control", "no-cache");
        response.getOutputStream().write(buffer);
        response.flushBuffer();

    }
```