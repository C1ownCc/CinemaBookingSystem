<template>
    <div class="ticket-check">
      <h2>检票系统</h2>
      <input type="text" v-model="ticketCode" placeholder="输入观影码" />
      <button @click="checkTicket">检票</button>
    </div>
  </template>
  
  <script>
  export default {
    data() {
      return {
        ticketCode: '',
      };
    },
    methods: {
        async checkTicket() {
            try {
                const response = await axios.put('/sysBill/inspection', null, {
                    params: {
                        code: this.ticketCode,
                    }
                });
                this.checkResult = response.data;
                if (response.data === '检票成功！') {
                    this.$message.success('验证通过！');
                    this.ticketCode = '';
                }else{
                    this.$message.error(response.data);
                }
            } catch (error) {
                console.error('Error checking ticket:', error);
            }
        },
    },
  };
  </script>
  
  <style scoped>
  html, body {
    margin: 0;
    padding: 0;
    width: 100%;
    height: 100%;
    overflow: hidden; /* 隐藏滚动条 */
    display: flex;
    justify-content: center;
    align-items: center;
    background-color: #f0f0f0; /* 背景颜色 */
  }
  
  .ticket-check {
    display: flex;
    width: 90%;
    height: 90%;
    /* max-width: 600px;
    max-height: 600px; */
    margin: auto;
    padding: 5%;
    border-radius: 4px;
    text-align: center;
    background-color: white;
    box-sizing: border-box; /* 包括内边距和边框在内的宽度计算 */
    overflow: auto; /* 在内容溢出时显示滚动条 */
    align-items: center;
    flex-direction: column;
    justify-content: center;
  }
  
  .ticket-check input {
    width: calc(100% - 22px);
    padding: 10px;
    margin-bottom: 10px;
    border: 1px solid #ccc;
    border-radius: 4px;
  }
  
  .ticket-check button {
    padding: 10px 20px;
    background-color: #4CAF50;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
  }
  
  .ticket-check button:hover {
    background-color: #45a049;
  }
  </style>
  