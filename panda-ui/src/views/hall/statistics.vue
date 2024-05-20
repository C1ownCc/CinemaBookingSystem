<template>
    <div>
      <!--面包屑导航区域-->
      <div class="board">
        <el-breadcrumb separator-class="el-icon-arrow-right">
          <el-breadcrumb-item :to="{ path: '/welcome' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>场次管理</el-breadcrumb-item>
          <el-breadcrumb-item>上座率统计</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
  
      <!--卡片视图-->
      <el-card class="box-card">
        <el-row :gutter="20">
          <el-col :span="4">
            <el-select v-model="selectedHallId" placeholder="请选择影厅名称" clearable>
              <el-option
                  v-for="item in hallList"
                  :key="item.hallId"
                  :label="item.hallName"
                  :value="item.hallId">
              </el-option>
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-select v-model="selectedMovieId" placeholder="请选择电影名称" clearable>
              <el-option
                  v-for="item in movieList"
                  :key="item.movieId"
                  :label="item.movieName"
                  :value="item.movieId">
              </el-option>
            </el-select>
          </el-col>
  
          <el-col :span="5">
            <el-date-picker
                v-model="selectedDate"
                value-format="yyyy-MM-dd"
                type="date"
                placeholder="选择日期">
            </el-date-picker>
          </el-col>
  
          <el-col :span="2">
            <el-button icon="el-icon-search" @click="getSessionList">搜索</el-button>
          </el-col>
        </el-row>
  
        <!--影厅分类列表-->
        <el-table :data="sessionList" style="width: 100%" border stripe >
          <el-table-column type="selection" width="55"></el-table-column>
          <el-table-column prop="sessionId" label="#" width="40"></el-table-column>
          <el-table-column prop="sysHall.hallName" label="影厅名称"></el-table-column>
          <el-table-column prop="sysMovie.movieName" label="电影名称"></el-table-column>
          <el-table-column prop="sessionDate" label="场次时间" width="200"></el-table-column>  
          <el-table-column label="操作" width="180" align="center">
            <template slot-scope="scope">
              <el-tooltip effect="dark" content="查看统计图" placement="top" :enterable="false" :open-delay="500">
                <el-button type="warning" icon="el-icon-setting" size="mini" @click="checkSeat(scope.row.sessionId)"></el-button>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
  
        <!--分页区域-->
        <el-pagination
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="queryInfo.pageNum"
            :page-sizes="[5, 7, 9]"
            :page-size="queryInfo.pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total">
        </el-pagination>
  
      </el-card>

      <!--安排座位对话框-->
      <el-dialog title="查看座位" :visible.sync="checkDialogVisible" :width="checkDialogWidth">
        <div ref="pie" style="width: 100%; height: 400px; margin-top: 20px;"></div>
      </el-dialog>
  
  
    </div>
  </template>
  
  <script>
  import global from "@/assets/css/global.css"
  import * as echarts from 'echarts'
  import moment from 'moment'
  export default {
    name: "HallInfo",
    data() {
      return {
        queryInfo: {
          hallId: '',
          movieId: '',
          sessionDate: '',
          pageNum: 1,
          pageSize: 7
        },
        sessionList: [],
        total: 0,
        
        checkAbleId: {},
        selectedHallId: '',
        endPlayTime: '',
        selectedMovieId: '',
        selectedDate: '',
        hallList: [],
        movieList: [],
        multipleSelection: [],
        checkDialogVisible: false,
        seats: {},
        col: '',
        isSelected: {
          0: 'seat-default',
          1: 'seat-choose',
          3: 'seat-sold'
        },
        editSeat: {},
        checkDialogWidth: '',
        pickerOptions: {
          disabledDate(time) {
            return time.getTime() > Date.now();
          }
        },
        rowNums: '',
        colNums: '',
        rowStart: '',
        loginUser: JSON.parse(window.sessionStorage.getItem('loginUser'))
      }
    },
    created() {
      this.getSessionList()
      this.getHallList()
      this.getMovieList()
      // this.curCinemaName = this.loginUser.cinemaName
    },
    methods: {
      drawPieChart(used, total) {
        let chartElement = this.$refs['pie'];
        if (!chartElement) {
          console.error("无法找到元素 #pie-chart");
          return;
        }

        let chart = echarts.init(chartElement);
        let option = {
          title: {
            text: '上座率',
            left: 'center'
          },
          tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b}: {c} ({d}%)'
          },
          legend: {
            orient: 'vertical',
            left: 'left'
          },
          series: [
            {
              name: '座位情况',
              type: 'pie',
              radius: '50%',
              data: [
                { value: used, name: '已入座' },
                { value: total - used, name: '未入座' }
              ],
              label: {
                normal: {
                  formatter: '{b}: {c} ({d}%)',
                  position: 'outside'
                }
              },
              emphasis: {
                itemStyle: {
                  shadowBlur: 10,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(0, 0, 0, 0.5)'
                }
              }
            }
          ]
        };
        chart.setOption(option);
      },
      getSessionList() {
        this.queryInfo.hallId = this.selectedHallId
        this.queryInfo.movieId = this.selectedMovieId
        this.queryInfo.sessionDate = this.selectedDate
        console.log(String(this.selectedDate))
        const _this = this;
        axios.get('sysSession', {params: _this.queryInfo}).then(resp => {
          console.log(resp)
          _this.sessionList = resp.data.data;
          _this.total = resp.data.total;
          _this.queryInfo.pageSize = resp.data.pageSize;
          _this.queryInfo.pageNum = resp.data.pageNum;
        })
      },
      handleSizeChange(newSize) {
        this.queryInfo.pageSize = newSize
        this.getSessionList()
        console.log(newSize)
      },
      handleCurrentChange(newPage) {
        this.queryInfo.pageNum = newPage
        this.getSessionList()
        console.log(newPage)
      },
      
      getHallList(){
        const _this = this
        axios.get('sysHall').then(resp => {
          _this.hallList = resp.data.data;
        })
      },
      getMovieList(){
        const _this = this
        axios.get('sysMovie/find').then(resp => {
          _this.movieList = resp.data.data;
        })
      },
      // async checkSeat(id){
      //   const _this = this
      //   let seatNums = 0;
      //   let sallNums = 0;
      //   await axios.get('sysSession/find/' + id).then(resp => {
      //     console.log(resp)
      //     _this.seats = JSON.parse(resp.data.data.sessionSeats)
      //     _this.col = resp.data.data.sysHall.seatNumsRow
      //     seatNums = resp.data.data.sysHall.seatNums
      //     sallNums = resp.data.data.sallNums
      //   })
      //   // console.log(this.col)
      //   this.checkDialogWidth = 200 + this.col * 40 + 'px'
      //   this.checkDialogVisible = true
      //   this.$nextTick(() => {
      //     _this.drawPieChart(sallNums, seatNums);
      //   });
      // }
      async checkSeat(id) {
      let usedSeats = 0;
      let totalSeats = 0;
      await axios.get('sysBill/occupancy/' + id).then(resp => {
        usedSeats = resp.data.data.usedSeats;
        totalSeats = resp.data.data.totalSeats;
      })
      this.checkDialogWidth = '600px';
      this.checkDialogVisible = true
      this.$nextTick(() => {
        this.drawPieChart(usedSeats, totalSeats);
      });
    }
    }
  }
  </script>
  
  <style scoped>
  .row{
    white-space: nowrap;
    margin-top: 10px;
    padding: 0 10px;
    text-align: center;
    display: flex;
    justify-content: space-between;
  }
  .seat{
    display: inline-block;
    width: 30px;
    height: 26px;
    margin: 0 5px;
    background-position: 0 1px;
  }
  .seat-default {
    background: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAeCAYAAAA7MK6iAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAAEnQAABJ0Ad5mH3gAAAHKSURBVEhLxZfbysIwDMezKeJUVAR9A/H9X0FQZMOLeem14sWc4AnP9muybM4557eT/mCYdLT/tknaqQgJ/ACVf7/Oz4TfbvVisYDlcgm1Wg2SRON0OoGqqtDr9bglAAoHGY1GQgqzl5zz+SyGwyF7z7wITyYTtrJD13W2HrzEuFqtspUdnU4Hbrcbew5fSa5SqfQbYbmzoCgKew5fK6disciWgyd8vV5hPB5DoVAA27bpSct6vQbLsmjFhmFwK0MpJjFNky2H3W4nZEf24rPZbMTlcmFPiPv9LgaDAXu+rNY0jS0HzG5Zh+zFRwo9bS/GuNvtgpwM+SQsVwetVosa/GDnpMhFsfUAJ3I4HMgmYRQIZl1awsbzTyYyqzHRkhK2Yj+RwklwBYMHRpDMhd0tTrXiT52jwCsxCrqPZc3RARLM7O1266V/XMrlMlQqFfYcVqsVZXa9Xo8Wzhq/cOYx/i+e8KeYZIFfgyyMxWw2o4Y8mc/nXty9jz08NqfTKTSbTapBLAt8kh6buDocGh+3xNrtNjQaDbLxRSj7/f7pNolLv98Xx+ORvVfeBla+S3VJ4K7hGO/I7S8MDht2UbjklspRokj+NRQKwB/pWISi3oSUQQAAAABJRU5ErkJggg==") no-repeat;
  }
  .seat-choose{
    background: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAeCAYAAAA7MK6iAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAAEnQAABJ0Ad5mH3gAAAKVSURBVEhLxZXPThpRFMY/hhFBG4okbdiYxgXpxmJi0phi4sKlL+HWlSsfwMSFr+DKjSufwMSFiyakaaagQaOxW1txUUjDHwVh5vacmdtxgGEsA1N/yYHLvdzzzT33nDMhQeAFUOT3f+fFhAeGutRs4tfpKV5dXECoqpz9R8hlKxqFsrqK96mUnOzGVfhrsYh3+/tIXV8Dk5Nydkh0HW3a+21zE59WVuTkE33C5/k8PmxvW4ITE/SPkFzxQacDUOS0nR18XFiQkxZ9dzx9dARwaCOR0UQZ9kP29vAQupz6S39y8UmVMeZcOIwInfp54QAQdJDe2LkLt9tAq2Vmp294L/vhe6Zxb13YwrSMvKZRZMIoLy2hvLYG3N/7E+c9FN7f2SzK8/MQiQS0szO5aGFndfHkBJndXSAeNzc2ZmehUw3GCwUru4eBTlpbXETs6grqwwNgGBBkX7a2kF1eNv9inziWy1minFxU/NM3N2jHYr5PbJAPtdGwHpp8hiiS6YMDUPBNTOE6WfL21sxAGx7Xav5KivaIx0fy7kghGqv0QHR+6yd/GGQUcx52M0Idu+0UDn+ORxovLp24i8CEO9SrvQhM+LlL8hQWnJHcBHxYiKvDA7OOqzTobGwgWalYjZ2hjlObm0N7Zmb4kqIkipZKmLq7e6oU8ldJJqHu7YGK1kOYke3OF1xKzvLsEfa+Y34IDrcfc4q6YAsrfDcBozgy3RSeIvuRyVidinsrNfixW7WKn+m0qcXYLwlum9+Pj5G4vITO709KEDaDmrsfFPLBrtnMrkhvqDfr63gt13nBlYZhiFyhIH8Nz2dNE005dmNgcgl6Fxv8dvGJXq9DcIgHYId63LBbvqpBeJfTCHiJMoEJewP8Afy6sw903o8jAAAAAElFTkSuQmCC") no-repeat;
  }
  .seat-sold{
    background: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAeCAYAAAA7MK6iAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAAEnQAABJ0Ad5mH3gAAANwSURBVEhLxZZLTBNRFIb/mWlpjbWAYgISNBofGyMGwQdEg0sTdaciaNy40UTxmeiKuDPGxwZXsjAuDNHohmhcYARjjGJBoYSFhAUYA+EpSksfQ8dz7gwwbacj5SEfXMq9c+757z3n3DuVNALLgGx8/neWTThlqIMjgL+3Gd3B11AklzE6NzTEkKFloTT/LNZtzDJG47EUbm/twouhk+hTOpAhGYNpMkXNSX+qPO+wv6xcHzSRJNzW/hm1A3vhcgD0C2mewswUeQ6T+IUcPwqLthujOkk5bh68AycpOklwIaKMQvMdCvBqsNoYmSVJOENeiQXqxcECEYzrHRP/paplKVnGUlil3ERiVJ2W9Z4atg9Gab4xN0qfXGRWB2dWmJ598bVAURzY4T6CA9mXEUpDnO1GQ8D5nfXYlFmMICkWeSux2XEQ3ow8fPX5DUudmapuamlA3dhReKiweCTXsRVrXdvQFWyAYw5JH54ErhY/pfqQ8aCtAnuyD6Mn0IgQ/bBAjBZyxvMBZaVlwn5mx22jT4Qon1sXjQ6o3+GWssWkKE0KUAitds9jLHqtuF707/oq4HGyDy8mtZA4HexToepuGD8tbBhdOAIMSa1xCeejMBEZFvkqyTuGm7ubMEKhNIvz/xxe3qlG2bzvq8Qat34MI1NBKirDkBC+ZRW0FoGuxbmkiSY7gUTV6CSL9z+e4+dEB2r2NYrdTYvzQq4Y4b3nq8JqQ5SRLC8Baea5LmxlY8JLV/Uj/0X0Bzpxq6xRCCaGN2fFrChjVclmzNG1hXdT57+E/okuXC95hnOFtXHhTURVqShssBVOXDWL885/h/vhdmSRaHx44/hHFG2FnbJbXCbmxmF/3FWNh+2nkEmifEEk2nBzKR5xIlKhn+MwcKNpA/44+kQ1M/xmyXfuglcpIAdUfWkgQ8Gg2olhtTvO3yp1PW6X99KqbIQZNrZbtR3sJtGXWdg21DyRb635NLOoFUKYa0ilq22piSE6cwcIYYmKZAuOI0CpDHOjh4vdJshvQeyQ0BKaIseERnl+2/oSPeE3kDWnuHm4xWLpFdY0siyL4yga7dXrysWJohpIdNEIWNiKqZCmtX7yG730afn4TdOiRseClMU1qQbopf7L6KVPIDyGkJq6bpK+ZS4W7Nb6RaFje5wWgp0os2TC9gB/AXsuAMA34noyAAAAAElFTkSuQmCC") no-repeat;
  }
  </style>
  