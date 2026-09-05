<template>
  <div class="addresses">
    <div class="header">
      <h2>收货地址</h2>
      <el-button type="primary" :disabled="loading" @click="openCreate">
        新增地址
      </el-button>
    </div>
    <div v-loading="loading" class="content">
      <el-alert
        v-if="errorMsg"
        type="error"
        :closable="false"
        show-icon
        :title="errorMsg"
        class="error"
      />
      <el-empty v-else-if="addresses.length === 0" description="暂无地址，点击右上角新增" />
      <ul v-else class="address-list">
        <li v-for="item in addresses" :key="item.id">
          <div class="address-head">
            <span class="receiver">{{ item.receiverName }} {{ item.receiverPhone }}</span>
            <el-tag v-if="item.isDefault === 1" type="success" size="small">默认</el-tag>
            <el-tag v-if="item.addressLabel" size="small">{{ item.addressLabel }}</el-tag>
          </div>
          <p class="detail">{{ item.addressDetail }}</p>
          <div class="actions">
            <el-button link type="primary" @click="openEdit(item)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(item)">删除</el-button>
          </div>
        </li>
      </ul>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑地址' : '新增地址'"
      width="480px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="最长 50 个字符" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="详细地址" prop="addressDetail">
          <el-input
            v-model="form.addressDetail"
            type="textarea"
            :rows="2"
            placeholder="最长 255 个字符"
          />
        </el-form-item>
        <el-form-item label="标签" prop="addressLabel">
          <el-input v-model="form.addressLabel" placeholder="可选，最长 20 个字符" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch
            v-model="defaultOn"
            :disabled="form.isDefault === 1 && editingId"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  createAddress,
  deleteAddress,
  fetchAddresses,
  updateAddress
} from '@/api/address'

const loading = ref(false)
const errorMsg = ref('')
const addresses = ref([])

const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref()
const defaultOn = ref(false)

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  addressDetail: '',
  addressLabel: '',
  isDefault: 0
})

// 校验规则与 api-contract.md 第 5 节一致
const rules = {
  receiverName: [
    { required: true, message: '请输入收货人姓名', trigger: 'blur' },
    { max: 50, message: '收货人最长 50 个字符', trigger: 'blur' }
  ],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[0-9]{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  addressDetail: [
    { required: true, message: '请输入详细地址', trigger: 'blur' },
    { max: 255, message: '详细地址最长 255 个字符', trigger: 'blur' }
  ],
  addressLabel: [{ max: 20, message: '标签最长 20 个字符', trigger: 'blur' }]
}

function resetForm() {
  editingId.value = null
  form.receiverName = ''
  form.receiverPhone = ''
  form.addressDetail = ''
  form.addressLabel = ''
  form.isDefault = 0
  defaultOn.value = false
  formRef.value?.clearValidate()
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(item) {
  editingId.value = item.id
  form.receiverName = item.receiverName
  form.receiverPhone = item.receiverPhone
  form.addressDetail = item.addressDetail
  form.addressLabel = item.addressLabel || ''
  form.isDefault = item.isDefault
  defaultOn.value = item.isDefault === 1
  dialogVisible.value = true
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    addresses.value = await fetchAddresses()
  } catch (error) {
    addresses.value = []
    errorMsg.value = error.response?.data?.msg || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    const payload = {
      receiverName: form.receiverName.trim(),
      receiverPhone: form.receiverPhone.trim(),
      addressDetail: form.addressDetail.trim(),
      addressLabel: form.addressLabel.trim() || null,
      isDefault: defaultOn.value ? 1 : 0
    }
    if (editingId.value) {
      await updateAddress(editingId.value, payload)
      ElMessage.success('地址已更新')
    } else {
      await createAddress(payload)
      ElMessage.success('地址已新增')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    // 字段错误等由拦截器统一提示，这里保留对话框以便修改
    ElMessage.error(error.response?.data?.msg || '保存失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(item) {
  try {
    await ElMessageBox.confirm(
      `确定删除“${item.receiverName}”的这条地址吗？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteAddress(item.id)
    ElMessage.success('地址已删除')
    await load()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '删除失败，请稍后重试')
  }
}

onMounted(load)
</script>

<style scoped>
.addresses {
  padding: 8px 0;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.content {
  min-height: 160px;
  margin-top: 8px;
}

.error {
  margin-bottom: 12px;
}

.address-list {
  list-style: none;
}

.address-list li {
  padding: 12px 14px;
  margin-bottom: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.address-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.receiver {
  font-weight: 700;
}

.detail {
  color: #606266;
  margin: 6px 0;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
